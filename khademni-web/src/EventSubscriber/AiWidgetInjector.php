<?php

namespace App\EventSubscriber;

use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\Event\ResponseEvent;
use Symfony\Component\HttpKernel\KernelEvents;

/**
 * Injects the AI Description Generator button + script into the contract form pages.
 *
 * This EventSubscriber approach avoids modifying ANY Twig template.
 * It detects contract form pages by URL and injects a small JS widget
 * that adds the AI button next to the description field.
 */
class AiWidgetInjector implements EventSubscriberInterface
{
    public static function getSubscribedEvents(): array
    {
        return [
            KernelEvents::RESPONSE => ['onKernelResponse', -10],
        ];
    }

    public function onKernelResponse(ResponseEvent $event): void
    {
        if (!$event->isMainRequest()) {
            return;
        }

        $request = $event->getRequest();
        $response = $event->getResponse();

        // Only inject on contract form pages (new + edit)
        $route = $request->attributes->get('_route', '');
        if (!in_array($route, ['app_contrat_new', 'app_contrat_edit'], true)) {
            return;
        }

        // Only inject on HTML responses
        $contentType = $response->headers->get('Content-Type', '');
        if (!str_contains($contentType, 'text/html') && !str_contains($contentType, 'html')) {
            // Also handle cases where content-type is not explicitly set
            $content = $response->getContent();
            if (false === $content || !str_contains($content, '</html>')) {
                return;
            }
        }

        $content = $response->getContent();
        if (false === $content) {
            return;
        }

        $jsWidget = $this->getAiWidgetScript();
        $milestoneHelper = $this->getMilestoneHelperScript();

        // Inject both widgets before closing </body> tag
        $content = str_replace('</body>', $jsWidget . "\n" . $milestoneHelper . "\n</body>", $content);
        $response->setContent($content);
    }

    private function getAiWidgetScript(): string
    {
        return <<<'HTML'
<script>
(function() {
    'use strict';

    // Find the description textarea (Symfony form widget)
    const descField = document.getElementById('contrat_description');
    if (!descField) return;

    // Create the AI button
    const wrapper = document.createElement('div');
    wrapper.style.cssText = 'margin-top: 8px; display: flex; align-items: center; gap: 10px;';

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.innerHTML = '✨ Générer avec IA';
    btn.style.cssText = `
        padding: 8px 18px;
        border: none;
        border-radius: 12px;
        background: linear-gradient(135deg, #6c5ce7 0%, #a855f7 100%);
        color: white;
        font-weight: 700;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.3s ease;
        box-shadow: 0 4px 12px rgba(108, 92, 231, 0.3);
        letter-spacing: 0.3px;
    `;
    btn.addEventListener('mouseenter', () => {
        btn.style.transform = 'translateY(-2px)';
        btn.style.boxShadow = '0 6px 20px rgba(108, 92, 231, 0.5)';
    });
    btn.addEventListener('mouseleave', () => {
        btn.style.transform = 'translateY(0)';
        btn.style.boxShadow = '0 4px 12px rgba(108, 92, 231, 0.3)';
    });

    const status = document.createElement('span');
    status.style.cssText = 'font-size: 12px; color: #9ca3af; font-weight: 500;';

    wrapper.appendChild(btn);
    wrapper.appendChild(status);
    descField.parentNode.appendChild(wrapper);

    // Add a hint input above the description
    const hintContainer = document.createElement('div');
    hintContainer.style.cssText = 'margin-bottom: 8px;';

    const hintLabel = document.createElement('label');
    hintLabel.textContent = '💡 Décrivez brièvement votre besoin (l\'IA fera le reste)';
    hintLabel.style.cssText = 'display: block; font-size: 12px; color: #6c5ce7; font-weight: 600; margin-bottom: 4px;';

    const hintInput = document.createElement('input');
    hintInput.type = 'text';
    hintInput.placeholder = 'Ex: site e-commerce Symfony, 5 pages, livraison 2 semaines...';
    hintInput.style.cssText = `
        width: 100%;
        padding: 10px 14px;
        border: 2px dashed #6c5ce7;
        border-radius: 10px;
        font-size: 13px;
        color: #1a1a1a;
        background: rgba(108, 92, 231, 0.04);
        outline: none;
        transition: border-color 0.3s;
        box-sizing: border-box;
    `;
    hintInput.addEventListener('focus', () => hintInput.style.borderColor = '#a855f7');
    hintInput.addEventListener('blur', () => hintInput.style.borderColor = '#6c5ce7');

    hintContainer.appendChild(hintLabel);
    hintContainer.appendChild(hintInput);
    descField.parentNode.insertBefore(hintContainer, descField);

    // Click handler
    btn.addEventListener('click', async () => {
        const hint = hintInput.value.trim();
        if (!hint) {
            hintInput.style.borderColor = '#ef4444';
            hintInput.focus();
            status.textContent = '⚠️ Veuillez d\'abord décrire votre besoin ci-dessus.';
            status.style.color = '#ef4444';
            setTimeout(() => {
                hintInput.style.borderColor = '#6c5ce7';
                status.textContent = '';
            }, 3000);
            return;
        }

        // Get title and price from the form
        const titleField = document.getElementById('contrat_titre');
        const priceField = document.getElementById('contrat_prix');
        const title = titleField ? titleField.value : '';
        const price = priceField ? parseFloat(priceField.value) || 0 : 0;

        btn.disabled = true;
        btn.innerHTML = '⏳ Génération en cours...';
        btn.style.opacity = '0.7';
        status.textContent = 'L\'IA analyse votre demande...';
        status.style.color = '#6c5ce7';

        try {
            const resp = await fetch('/api/ai/generate-description', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hint, title, price }),
            });

            const data = await resp.json();

            if (data.success) {
                descField.value = data.description;
                descField.style.borderColor = '#10b981';
                status.textContent = '✅ Description générée avec succès!';
                status.style.color = '#10b981';
                // Trigger input event for any listeners
                descField.dispatchEvent(new Event('input', { bubbles: true }));
                setTimeout(() => {
                    descField.style.borderColor = '';
                    status.textContent = '';
                }, 4000);
            } else {
                status.textContent = '❌ ' + (data.error || 'Erreur lors de la génération.');
                status.style.color = '#ef4444';
            }
        } catch (err) {
            status.textContent = '❌ Erreur réseau. Réessayez.';
            status.style.color = '#ef4444';
        }

        btn.disabled = false;
        btn.innerHTML = '✨ Générer avec IA';
        btn.style.opacity = '1';
    });
})();
</script>
HTML;
    }

    private function getMilestoneHelperScript(): string
    {
        return <<<'HTML'
<script>
(function() {
    'use strict';

    const descField = document.getElementById('contrat_description');
    if (!descField) return;

    // --- Update placeholder ---
    descField.placeholder = 'Décrivez le contrat...\n\n💡 Paiements par étapes ? Ajoutez à la fin :\n(Milestones: 30%/30%/40%)';

    // --- Build the helper card ---
    const card = document.createElement('div');
    card.style.cssText = `
        margin-top: 12px;
        background: linear-gradient(135deg, rgba(108,92,231,0.06) 0%, rgba(168,85,247,0.04) 100%);
        border: 1.5px solid rgba(108, 92, 231, 0.25);
        border-radius: 16px;
        padding: 16px 20px;
        font-family: 'Inter', 'Segoe UI', Arial, sans-serif;
    `;

    card.innerHTML = `
        <div style="display:flex; align-items:center; justify-content:space-between; cursor:pointer;" id="milestone-toggle">
            <div style="display:flex; align-items:center; gap:8px;">
                <span style="font-size:18px;">📋</span>
                <span style="font-size:13px; font-weight:800; color:#6c5ce7; letter-spacing:0.2px;">Paiements par Milestones (optionnel)</span>
                <span style="background:#6c5ce7; color:white; font-size:10px; font-weight:700; padding:2px 8px; border-radius:20px; letter-spacing:0.5px;">AVANCÉ</span>
            </div>
            <span id="milestone-chevron" style="color:#6c5ce7; font-size:16px; transition:transform 0.3s;">▾</span>
        </div>

        <div id="milestone-body" style="display:none; margin-top:14px;">
            <p style="font-size:12px; color:#6b7280; margin:0 0 12px; line-height:1.6;">
                Ajoutez <strong style="color:#1a1a1a; font-family:monospace;">(Milestones: X%/Y%/Z%)</strong> n'importe où dans la description.<br>
                Le total <strong>doit être égal à 100%</strong>. Le paiement se fait automatiquement étape par étape.
            </p>

            <div style="font-size:12px; font-weight:700; color:#374151; margin-bottom:8px;">Exemples rapides :</div>
            <div style="display:flex; gap:8px; flex-wrap:wrap; margin-bottom:14px;" id="milestone-presets">
                <button type="button" data-value="(Milestones: 50%/50%)" style="padding:6px 12px; border-radius:8px; border:1.5px solid rgba(108,92,231,0.3); background:white; font-size:12px; font-weight:700; color:#6c5ce7; cursor:pointer; transition:all 0.2s;">
                    2 étapes · 50/50
                </button>
                <button type="button" data-value="(Milestones: 30%/30%/40%)" style="padding:6px 12px; border-radius:8px; border:1.5px solid rgba(108,92,231,0.3); background:white; font-size:12px; font-weight:700; color:#6c5ce7; cursor:pointer; transition:all 0.2s;">
                    3 étapes · 30/30/40
                </button>
                <button type="button" data-value="(Milestones: 25%/25%/25%/25%)" style="padding:6px 12px; border-radius:8px; border:1.5px solid rgba(108,92,231,0.3); background:white; font-size:12px; font-weight:700; color:#6c5ce7; cursor:pointer; transition:all 0.2s;">
                    4 étapes · 25×4
                </button>
                <button type="button" data-value="(Milestones: 40%/60%)" style="padding:6px 12px; border-radius:8px; border:1.5px solid rgba(108,92,231,0.3); background:white; font-size:12px; font-weight:700; color:#6c5ce7; cursor:pointer; transition:all 0.2s;">
                    2 étapes · 40/60
                </button>
            </div>

            <div style="background:rgba(0,0,0,0.03); border-radius:10px; padding:12px 14px; font-size:12px; color:#374151; line-height:1.8; border:1px solid rgba(0,0,0,0.06);">
                <strong>💡 Comment ça marche :</strong><br>
                1. Ajoutez le format Milestones dans la description du contrat<br>
                2. Sauvegardez le contrat<br>
                3. Le bouton <strong>"Payer"</strong> lancera le paiement de l'étape 1 seulement<br>
                4. Une fois payée, le prochain clic paie l'étape 2, etc.
            </div>

            <div id="milestone-feedback" style="display:none; margin-top:10px; padding:8px 12px; border-radius:8px; font-size:12px; font-weight:700;"></div>
        </div>
    `;

    // Find the description field's parent and insert after the AI widget
    descField.parentNode.appendChild(card);

    // Toggle expand/collapse
    const toggle = card.querySelector('#milestone-toggle');
    const body = card.querySelector('#milestone-body');
    const chevron = card.querySelector('#milestone-chevron');

    toggle.addEventListener('click', () => {
        const isOpen = body.style.display !== 'none';
        body.style.display = isOpen ? 'none' : 'block';
        chevron.style.transform = isOpen ? '' : 'rotate(180deg)';
    });

    // Preset buttons — insert the milestone string into the description
    card.querySelectorAll('#milestone-presets button').forEach(btn => {
        btn.addEventListener('mouseenter', () => {
            btn.style.background = 'rgba(108,92,231,0.08)';
            btn.style.borderColor = '#6c5ce7';
        });
        btn.addEventListener('mouseleave', () => {
            btn.style.background = 'white';
            btn.style.borderColor = 'rgba(108,92,231,0.3)';
        });
        btn.addEventListener('click', () => {
            const val = btn.dataset.value;
            const currentText = descField.value.trim();

            // Remove any existing Milestones block first
            const cleaned = currentText.replace(/\s*\(Milestones:[^)]+\)/gi, '').trim();
            descField.value = cleaned ? cleaned + '\n\n' + val : val;
            descField.dispatchEvent(new Event('input', { bubbles: true }));

            // Flash feedback
            const feedback = card.querySelector('#milestone-feedback');
            feedback.style.display = 'block';
            feedback.style.background = 'rgba(16, 185, 129, 0.1)';
            feedback.style.color = '#059669';
            feedback.style.border = '1px solid rgba(16, 185, 129, 0.3)';
            feedback.textContent = '✅ Format "' + val + '" inséré dans la description !';
            setTimeout(() => { feedback.style.display = 'none'; }, 3000);
        });
    });
})();
</script>
HTML;
    }
}
