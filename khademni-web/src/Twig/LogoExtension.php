<?php

namespace App\Twig;

use App\Service\LogoService;
use Twig\Extension\AbstractExtension;
use Twig\TwigFunction;

class LogoExtension extends AbstractExtension
{
    public function __construct(
        private LogoService $logoService
    ) {
    }

    public function getFunctions(): array
    {
        return [
            new TwigFunction('brand_logo', [$this, 'getBrandLogo']),
        ];
    }

    /**
     * Returns an image URL: either a real brand logo or an SVG initials avatar.
     * Usage in Twig: {{ brand_logo('Microsoft') }}
     */
    public function getBrandLogo(string $name): string
    {
        $logo = $this->logoService->getLogoUrl($name);

        return $logo ?? $this->logoService->getInitialsAvatar($name);
    }
}
