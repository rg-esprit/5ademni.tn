<?php

namespace App\EventSubscriber;

use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\Event\ResponseEvent;
use Symfony\Component\HttpKernel\KernelEvents;

/**
 * Injects a Kebab Menu (Three-dot dropdown) into the Contract index cards.
 *
 * This reduces cognitive overload by hiding the `Edit`, `Pay`, and `PDF` buttons
 * inside a clean popup menu, without modifying the underlying Twig templates.
 */
class KebabMenuInjector implements EventSubscriberInterface
{
    public static function getSubscribedEvents(): array
    {
        return [
            // Priority is 0, so it runs after standard generic listeners, but before the response is fully sent
            KernelEvents::RESPONSE => ['onKernelResponse', 0],
        ];
    }

    public function onKernelResponse(ResponseEvent $event): void
    {
        if (!$event->isMainRequest()) {
            return;
        }

        $request = $event->getRequest();
        $response = $event->getResponse();

        // Only inject on the contract or payment index page
        $route = $request->attributes->get('_route');
        if (!in_array($route, ['app_contrat_index', 'app_payment_index'], true)) {
            return;
        }

        $contentType = $response->headers->get('Content-Type', '');
        if (!str_contains($contentType, 'text/html') && !str_contains($contentType, 'html')) {
            $content = $response->getContent();
            if (false === $content || !str_contains($content, '</html>')) {
                return;
            }
        }

        $content = $response->getContent();
        if (false === $content) {
            return;
        }

        $jsWidget = $this->getKebabScript();

        // Inject before closing </body> tag
        $content = str_replace('</body>', $jsWidget . "\n</body>", $content);
        $response->setContent($content);
    }

    private function getKebabScript(): string
    {
        return <<<'HTML'
<script>
document.addEventListener('DOMContentLoaded', () => {
    'use strict';
    
    // Find all contract glass cards
    const cards = document.querySelectorAll('.bento-grid-container > .glass-card');
    
    cards.forEach(card => {
        // Collect all action buttons inside the card
        const actionLinks = Array.from(card.querySelectorAll('a.button-primary'));
        
        // If the card doesn't have at least one valid button string, skip
        if (actionLinks.length === 0) return;
        
        // Get the wrapper div holding those buttons
        const actionContainer = actionLinks[0].parentNode;
        if (!actionContainer) return;
        
        // Create the Kebab outer wrapper
        const kebabWrapper = document.createElement('div');
        kebabWrapper.style.cssText = 'position: relative; display: inline-block;';
        
        // Create the Kebab Button trigger (⋮)
        const kebabBtn = document.createElement('button');
        kebabBtn.innerHTML = '&#8942;'; // 3 vertical dots HTML entity
        kebabBtn.title = 'Options du contrat';
        kebabBtn.style.cssText = `
            background: rgba(108, 92, 231, 0.08);
            color: #6c5ce7;
            border: 1px solid rgba(108, 92, 231, 0.2);
            border-radius: 50%;
            width: 38px;
            height: 38px;
            font-size: 22px;
            font-weight: 900;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: 0 2px 5px rgba(0,0,0,0.02);
            padding-bottom: 2px;
        `;
        
        // Hover effects for Kebab Button
        kebabBtn.onmouseenter = () => {
            kebabBtn.style.background = 'rgba(108, 92, 231, 0.15)';
            kebabBtn.style.transform = 'scale(1.05)';
        };
        kebabBtn.onmouseleave = () => {
            kebabBtn.style.background = 'rgba(108, 92, 231, 0.08)';
            kebabBtn.style.transform = 'scale(1)';
        };
        
        // Create the Dropdown Menu (Glassmorphism design)
        const dropdown = document.createElement('div');
        dropdown.classList.add('kebab-dropdown');
        dropdown.style.cssText = `
            position: absolute;
            bottom: 110%; /* Show above the button */
            right: 0;
            margin-bottom: 10px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.4);
            border-radius: 14px;
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12), 0 4px 10px rgba(108, 92, 231, 0.05);
            display: none;
            flex-direction: column;
            min-width: 170px;
            z-index: 1000;
            overflow: hidden;
            opacity: 0;
            transform: translateY(15px) scale(0.95);
            transform-origin: bottom right;
            transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        `;
        
        // Transfer the links into the dropdown as styled menu items
        actionLinks.forEach(link => {
            const menuItem = document.createElement('a');
            menuItem.href = link.href;
            if (link.target) menuItem.target = link.target;
            menuItem.innerHTML = link.innerHTML;
            
            menuItem.style.cssText = `
                padding: 14px 18px;
                text-decoration: none;
                color: #374151;
                font-family: 'Inter', 'Segoe UI', Arial, sans-serif;
                font-size: 14px;
                font-weight: 600;
                display: flex;
                align-items: center;
                gap: 10px;
                transition: background 0.2s ease, padding-left 0.2s ease;
                border-bottom: 1px solid rgba(0,0,0,0.04);
            `;
            
            // Hover animation for menu items
            menuItem.onmouseenter = () => {
                menuItem.style.background = 'rgba(108, 92, 231, 0.06)';
                menuItem.style.paddingLeft = '22px';
            };
            menuItem.onmouseleave = () => {
                menuItem.style.background = 'transparent';
                menuItem.style.paddingLeft = '18px';
            };
            
            // Apply semantic colors based on recognizable text/styles
            if (link.innerHTML.includes('Payer')) {
                menuItem.style.color = '#059669'; // Emerald text for payment
            } else if (link.innerHTML.includes('PDF')) {
                menuItem.style.color = '#2563eb'; // Blue text for export
            }
            
            dropdown.appendChild(menuItem);
        });
        
        // Remove trailing border on the last item for a clean look
        if(dropdown.lastChild) {
            dropdown.lastChild.style.borderBottom = 'none';
        }
        
        // Assemble component
        kebabWrapper.appendChild(kebabBtn);
        kebabWrapper.appendChild(dropdown);
        
        // Clean out original horizontal buttons, replace with our sleek Kebab
        actionContainer.innerHTML = '';
        actionContainer.appendChild(kebabWrapper);
        
        // Click listener for the Kebab
        kebabBtn.addEventListener('click', (e) => {
            e.stopPropagation(); // Prevents the global closer from firing immediately
            
            const isOpening = dropdown.style.display === 'none';
            
            // Force close any other open kebabs on the page
            document.querySelectorAll('.kebab-dropdown').forEach(d => {
                d.style.opacity = '0';
                d.style.transform = 'translateY(15px) scale(0.95)';
                setTimeout(() => { 
                    // Verify if it hasn't been reopened during timeout
                    if (d.style.opacity === '0') d.style.display = 'none'; 
                }, 250);
            });
            
            if (isOpening) {
                dropdown.style.display = 'flex';
                // Trigger a JS reflow to let CSS transitions work from 'none' -> 'flex'
                void dropdown.offsetWidth; 
                dropdown.style.opacity = '1';
                dropdown.style.transform = 'translateY(0) scale(1)';
            }
        });
        
        // Click anywhere outside hides the dropdown
        document.addEventListener('click', (e) => {
            if (!kebabWrapper.contains(e.target)) {
                if (dropdown.style.display !== 'none') {
                    dropdown.style.opacity = '0';
                    dropdown.style.transform = 'translateY(15px) scale(0.95)';
                    setTimeout(() => { dropdown.style.display = 'none'; }, 250);
                }
            }
        });
    });
});
</script>
HTML;
    }
}
