<?php

namespace App\Twig;

use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;

class BlobProxyExtension extends AbstractExtension
{
    public function getFilters(): array
    {
        return [
            new TwigFilter('proxy_blob', [$this, 'proxyBlob']),
        ];
    }

    public function proxyBlob(?string $url): ?string
    {
        if (!$url) {
            return $url;
        }

        // If it's a private Vercel Blob URL, route through proxy
        if (str_contains($url, 'private.blob.vercel-storage.com')) {
            return '/blob-proxy?url='.urlencode($url);
        }

        // Otherwise, return the URL as-is (public URLs or local paths)
        return $url;
    }
}
