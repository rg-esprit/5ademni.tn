<?php

namespace App\Tests\Entity;

use App\Entity\Message;
use PHPUnit\Framework\TestCase;

class MessageTest extends TestCase
{
    public function testMessageDetectsAttachment(): void
    {
        $message = new Message();

        self::assertFalse($message->hasAttachment());

        $message->setPieceJointeUrl('/uploads/messages/file.pdf');

        self::assertTrue($message->hasAttachment());
    }
}
