<?php
echo "Testing shell_exec:\n";
echo shell_exec('pdftotext -v 2>&1');
echo "\nTesting composer:\n";
echo shell_exec('composer --version 2>&1');
