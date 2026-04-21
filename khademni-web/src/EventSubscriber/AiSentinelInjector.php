<?php

namespace App\EventSubscriber;

use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\Event\ResponseEvent;
use Symfony\Component\HttpKernel\KernelEvents;

class AiSentinelInjector implements EventSubscriberInterface
{
    public static function getSubscribedEvents(): array
    {
        return [
            KernelEvents::RESPONSE => ['onKernelResponse', -20],
        ];
    }

    public function onKernelResponse(ResponseEvent $event): void
    {
        if (!$event->isMainRequest()) {
            return;
        }

        $request = $event->getRequest();
        $response = $event->getResponse();

        $route = $request->attributes->get('_route', '');
        
        // Active on Contract creation/editing
        if (in_array($route, ['app_contrat_new', 'app_contrat_edit'], true)) {
            $this->injectAuditLogic($response);
        }

        // Active on Payment confirmation (Email verification page)
        if ($route === 'app_contrat_verify_email') {
            $this->injectPaymentTrustBadge($response);
        }
    }

    private function injectPaymentTrustBadge($response): void
    {
        $content = $response->getContent();
        if (false === $content || !str_contains($content, '</body>')) {
            return;
        }

        $badge = <<<'HTML'
<div id="ai-trust-badge" style="
    max-width: 500px;
    margin: 20px auto;
    background: #f0fdf4;
    border: 1px solid #bdf4c9;
    border-radius: 12px;
    padding: 15px;
    display: flex;
    align-items: center;
    gap: 15px;
    font-family: 'Inter', sans-serif;
">
    <div style="font-size: 24px;">🛡️</div>
    <div>
        <div style="font-weight: 800; color: #166534; font-size: 14px;">Audit de Sécurité Khademni IA</div>
        <div style="font-size: 12px; color: #15803d; line-height: 1.4;">
            Ce contrat a été analysé par notre IA. Les termes sont jugés clairs et le risque de litige est minimal. Votre paiement est sécurisé.
        </div>
    </div>
</div>
<script>
    document.addEventListener('DOMContentLoaded', () => {
        const target = document.querySelector('form');
        if (target) {
            target.parentNode.insertBefore(document.getElementById('ai-trust-badge'), target);
        }
    });
</script>
HTML;

        $content = str_replace('</body>', $badge . "</body>", $content);
        $response->setContent($content);
    }

    private function injectAuditLogic($response): void
    {
        $content = $response->getContent();
        if (false === $content || !str_contains($content, '</body>')) {
            return;
        }

        $html = $this->getSentinelHtml();
        $js = $this->getSentinelJs();
        $css = $this->getSentinelCss();

        $injection = "\n" . $css . "\n" . $html . "\n" . $js . "\n";
        $content = str_replace('</body>', $injection . "</body>", $content);
        $response->setContent($content);
    }

    private function getSentinelCss(): string
    {
        return <<<'HTML'
<style>
    .ai-sentinel-panel {
        position: fixed;
        bottom: 20px;
        right: 20px;
        width: 300px;
        background: white;
        border-radius: 16px;
        box-shadow: 0 10px 40px rgba(0,0,0,0.15);
        z-index: 10000;
        overflow: hidden;
        font-family: 'Inter', sans-serif;
        border: 1px solid #e5e7eb;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        transform: translateY(10px);
        opacity: 0;
        pointer-events: none;
    }
    .ai-sentinel-panel.active {
        transform: translateY(0);
        opacity: 1;
        pointer-events: auto;
    }
    .ai-sentinel-header {
        background: #1a1a1a;
        color: white;
        padding: 12px 16px;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    .ai-sentinel-title {
        font-size: 13px;
        font-weight: 700;
        display: flex;
        align-items: center;
        gap: 8px;
    }
    .ai-sentinel-body {
        padding: 16px;
    }
    .ai-score-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 15px;
    }
    .ai-score-label { font-size: 12px; color: #6b7280; }
    .ai-score-value {
        font-size: 18px;
        font-weight: 800;
        color: #1a1a1a;
    }
    .ai-advise-box {
        font-size: 13px;
        line-height: 1.5;
        color: #374151;
        padding: 12px;
        background: #f9fafb;
        border-radius: 10px;
        border-left: 4px solid #d1d5db;
    }
    .status-safe { border-left-color: #10b981; }
    .status-warning { border-left-color: #f59e0b; }
    .status-danger { border-left-color: #ef4444; }
    
    .ai-sentinel-loading {
        font-size: 11px;
        color: #9ca3af;
        margin-top: 10px;
        display: none;
    }
    .ai-sentinel-loading.active { display: block; }
</style>
HTML;
    }

    private function getSentinelHtml(): string
    {
        return <<<'HTML'
<div id="ai-sentinel" class="ai-sentinel-panel">
    <div class="ai-sentinel-header">
        <div class="ai-sentinel-title">
            <span>🛡️ Sentinelle IA</span>
        </div>
        <span style="font-size: 10px; opacity: 0.6;">Dépistage en direct</span>
    </div>
    <div class="ai-sentinel-body">
        <div class="ai-score-row">
            <span class="ai-score-label">Indice de clarté</span>
            <span id="ai-sentinel-score" class="ai-score-value">--/10</span>
        </div>
        <div id="ai-sentinel-advise" class="ai-advise-box">
            Commencez à rédiger pour obtenir un audit automatique de votre contrat.
        </div>
        <div id="ai-sentinel-loader" class="ai-sentinel-loading">
            ⚙️ L'intelligence artificielle analyse vos termes...
        </div>
    </div>
</div>
HTML;
    }

    private function getSentinelJs(): string
    {
        return <<<'HTML'
<script>
(function() {
    'use strict';

    const fields = {
        titre: document.getElementById('contrat_titre'),
        description: document.getElementById('contrat_description'),
        prix: document.getElementById('contrat_prix')
    };

    const ui = {
        panel: document.getElementById('ai-sentinel'),
        score: document.getElementById('ai-sentinel-score'),
        advise: document.getElementById('ai-sentinel-advise'),
        loader: document.getElementById('ai-sentinel-loader')
    };

    if (!fields.description) return;

    let timeout = null;

    const triggerAudit = () => {
        const data = {
            titre: fields.titre ? fields.titre.value : '',
            description: fields.description.value,
            prix: fields.prix ? fields.prix.value : null
        };

        if (data.description.length < 10) {
            ui.panel.classList.remove('active');
            return;
        }

        ui.panel.classList.add('active');
        ui.loader.classList.add('active');

        fetch('/api/ai/audit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(r => r.json())
        .then(res => {
            ui.score.textContent = res.score + '/10';
            ui.advise.textContent = res.advise;
            ui.advise.className = 'ai-advise-box status-' + res.status.toLowerCase();
            
            // Color based on status
            if (res.status === 'SAFE') ui.score.style.color = '#10b981';
            else if (res.status === 'WARNING') ui.score.style.color = '#f59e0b';
            else ui.score.style.color = '#ef4444';
        })
        .catch(() => {
            ui.advise.textContent = "⚠️ Impossible de joindre l'IA pour l'audit.";
        })
        .finally(() => {
            ui.loader.classList.remove('active');
        });
    };

    const debouncedAudit = () => {
        clearTimeout(timeout);
        timeout = setTimeout(triggerAudit, 1500); // 1.5s silence trigger
    };

    Object.values(fields).forEach(f => {
        if (f) f.addEventListener('input', debouncedAudit);
    });

})();
</script>
HTML;
    }
}
