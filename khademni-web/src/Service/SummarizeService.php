<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class SummarizeService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        #[Autowire('%env(string:OPENROUTER_API_KEY)%')]
        private readonly string $apiKey,
        #[Autowire('%env(string:OPENROUTER_URL)%')]
        private readonly string $apiUrl,
    ) {
    }

    public function summarizeMessages(array $messages): string
    {
        if ([] === $messages) {
            return 'Aucun message à résumer.';
        }

        // If API is not configured, use local summary immediately
        if ('' === trim($this->apiKey)) {
            return $this->generateLocalSummary($messages);
        }

        // Try AI summary first, fallback to local if it fails
        try {
            $conversationText = '';
            $typeStats = [];
            $senderStats = [];

            foreach ($messages as $message) {
                $type = strtoupper(trim($message['typeMessage'] ?? $message['type'] ?? 'TEXTE'));
                if ('' === $type) {
                    $type = 'TEXTE';
                }

                $sender = trim($message['senderName'] ?? 'Inconnu');
                $content = trim($message['contenu'] ?? '');
                $attachment = trim($message['pieceJointeUrl'] ?? '');
                $duration = trim($message['dureeAudio'] ?? '');

                if ($type === 'TEXTE' && empty($content) && $attachment) {
                    $type = 'PIÈCE_JOINTE';
                }

                if ($type === 'TEXTE' && preg_match('/https?:\/\//i', $content)) {
                    $type = 'URL';
                }

                $contentSummary = $content;
                if ($attachment) {
                    $contentSummary = trim($contentSummary . ($contentSummary !== '' ? ' | ' : '') . 'URL/Pièce jointe: ' . $attachment);
                }
                if ($duration && in_array($type, ['AUDIO', 'VOCAL', 'PIÈCE_JOINTE'], true)) {
                    $contentSummary .= ' (Durée: ' . $duration . ')';
                }
                if ('' === $contentSummary) {
                    $contentSummary = '[sans contenu texte]';
                }

                $conversationText .= sprintf("[%s] %s (%s): %s\n",
                    $message['dateEnvoi'] ?? '',
                    $sender,
                    $type,
                    $contentSummary
                );

                if (!isset($typeStats[$type])) {
                    $typeStats[$type] = 0;
                }
                $typeStats[$type]++;

                if (!isset($senderStats[$sender])) {
                    $senderStats[$sender] = 0;
                }
                $senderStats[$sender]++;
            }

            $statsText = "\n\n**Statistiques de la conversation:**\n";
            $statsText .= sprintf("- Total: %d messages\n", count($messages));
            $statsText .= sprintf("- Participants: %d\n", count($senderStats));
            $statsText .= "\n**Messages par expéditeur:**\n";
            foreach ($senderStats as $sender => $count) {
                $statsText .= sprintf("- %s: %d messages\n", $sender, $count);
            }
            $statsText .= "\n**Types de messages:**\n";
            foreach ($typeStats as $type => $count) {
                $statsText .= sprintf("- %s: %d\n", $type, $count);
            }

            $prompt = sprintf("Analyse cette conversation et fournis un résumé détaillé en français.

FORMAT ATTENDU:
1. **Résumé général**: Un paragraphe concis sur le contenu de la conversation
2. **Participants**: Liste des expéditeurs avec leur nombre de messages
3. **Types de messages**: Répartition par type (TEXTE, IMAGE, URL, AUDIO/VOCAL, etc.)
4. **Contenu et sujet général**: Écris le sujet principal de la discussion et le contenu expliqué
5. **Points clés**: Maximum 3 points importants de la discussion

Voici les messages de la conversation:
%s

%s", $conversationText, $statsText);

            $response = $this->httpClient->request('POST', $this->apiUrl, [
                'headers' => [
                    'Authorization' => 'Bearer '.$this->apiKey,
                    'Content-Type' => 'application/json',
                    'HTTP-Referer' => 'https://5ademni.tn',
                    'X-Title' => '5ademni.tn Messaging',
                ],
                'json' => [
                    'model' => 'google/gemma-3-27b-it:free',
                    'messages' => [
                        [
                            'role' => 'user',
                            'content' => $prompt,
                        ],
                    ],
                    'temperature' => 0.4,
                    'max_tokens' => 450,
                ],
                'timeout' => 10,
            ]);

            $data = $response->toArray(false);

            if (isset($data['choices'][0]['message']['content']) &&
                is_string($data['choices'][0]['message']['content']) &&
                '' !== trim($data['choices'][0]['message']['content'])) {
                return trim($data['choices'][0]['message']['content']);
            }

            return $this->generateLocalSummary($messages);
        } catch (\Throwable $exception) {
            return $this->generateLocalSummary($messages);
        }
    }

    public function generateLocalSummary(array $messages): string
    {
        if ([] === $messages) {
            return 'Aucun message à résumer.';
        }

        $typeStats = [];
        $senderStats = [];
        $allContent = [];
        $participants = [];
        $attachmentTypes = [];

        foreach ($messages as $message) {
            $type = strtoupper(trim($message['typeMessage'] ?? $message['type'] ?? 'TEXTE'));
            if ('' === $type) {
                $type = 'TEXTE';
            }

            $sender = trim($message['senderName'] ?? 'Inconnu');
            $content = trim($message['contenu'] ?? '');
            $attachment = trim($message['pieceJointeUrl'] ?? '');

            if ($type === 'TEXTE' && preg_match('/https?:\/\//i', $content)) {
                $type = 'URL';
            }

            if ($type === 'TEXTE' && !empty($attachment)) {
                $type = 'PIÈCE_JOINTE';
            }

            if (!isset($typeStats[$type])) {
                $typeStats[$type] = 0;
            }
            $typeStats[$type]++;

            if (!isset($senderStats[$sender])) {
                $senderStats[$sender] = 0;
            }
            $senderStats[$sender]++;

            if (!empty($content)) {
                $allContent[] = $content;
            }
            if (!in_array($sender, $participants, true)) {
                $participants[] = $sender;
            }

            if (!empty($attachment)) {
                $attachmentTypes[] = $type;
            }
        }

        $summary = "**Résumé de la conversation**\n\n";
        $summary .= "📊 **Statistiques générales:**\n";
        $summary .= "- Total des messages: " . count($messages) . "\n";
        $summary .= "- Nombre de participants: " . count($participants) . "\n\n";
        $summary .= "👥 **Messages par expéditeur:**\n";
        foreach ($senderStats as $sender => $count) {
            $summary .= "- $sender: $count messages\n";
        }
        $summary .= "\n📝 **Types de messages:**\n";
        foreach ($typeStats as $type => $count) {
            $summary .= "- $type: $count\n";
        }
        $summary .= "\n";

        if (!empty($allContent)) {
            $summary .= "💬 **Contenu de la conversation:**\n";
            $firstMessage = $allContent[0];
            $firstMessage = strlen($firstMessage) > 100 ? substr($firstMessage, 0, 100) . '...' : $firstMessage;
            $summary .= "- Début: \"$firstMessage\"\n";
            $lastMessage = end($allContent);
            $lastMessage = strlen($lastMessage) > 100 ? substr($lastMessage, 0, 100) . '...' : $lastMessage;
            $summary .= "- Fin: \"$lastMessage\"\n";

            $words = [];
            foreach ($allContent as $content) {
                preg_match_all('/\b\w{6,}\b/u', $content, $matches);
                foreach ($matches[0] as $word) {
                    $word = strtolower($word);
                    if (!isset($words[$word])) {
                        $words[$word] = 0;
                    }
                    $words[$word]++;
                }
            }

            arsort($words);
            $topKeywords = array_slice(array_keys($words), 0, 3);
            if (!empty($topKeywords)) {
                $summary .= "- Sujets principaux: " . implode(', ', $topKeywords) . "\n";
            }
        }

        if (!empty($attachmentTypes)) {
            $summary .= "\n📎 **Médias/attachements présents:**\n";
            $attachmentCounts = array_count_values($attachmentTypes);
            foreach ($attachmentCounts as $type => $count) {
                $summary .= "- $type: $count\n";
            }
        }

        if (!empty($topKeywords)) {
            $subject = 'La discussion porte principalement sur ' . implode(', ', $topKeywords) . '.';
        } elseif (!empty($attachmentTypes)) {
            $subject = 'Le sujet général est un échange de ' . implode(', ', array_unique($attachmentTypes)) . '.';
        } else {
            $subject = 'Le sujet général est une conversation textuelle avec des demandes et réponses.';
        }

        $summary .= "\n🎯 **Sujet général:** " . $subject . "\n";

        return $summary;
    }
}
