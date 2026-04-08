<?php
$pdo = new PDO('mysql:host=127.0.0.1;dbname=appdb', 'root', '');
echo "=== CONTRATS TABLE ===\n";
foreach($pdo->query('SHOW COLUMNS FROM contrats') as $r) {
    echo $r['Field'] . ' | ' . $r['Type'] . ' | ' . $r['Null'] . ' | ' . $r['Key'] . ' | ' . $r['Default'] . "\n";
}
echo "\n=== PAYMENTS TABLE ===\n";
foreach($pdo->query('SHOW COLUMNS FROM payments') as $r) {
    echo $r['Field'] . ' | ' . $r['Type'] . ' | ' . $r['Null'] . ' | ' . $r['Key'] . ' | ' . $r['Default'] . "\n";
}
echo "\n=== SAMPLE CONTRATS ===\n";
foreach($pdo->query('SELECT * FROM contrats LIMIT 2') as $r) {
    print_r($r);
}
echo "\n=== SAMPLE PAYMENTS ===\n";
foreach($pdo->query('SELECT * FROM payments LIMIT 2') as $r) {
    print_r($r);
}
