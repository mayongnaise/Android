<?php
    $base=$_POST['image'];
	$filename=$_POST['filename'];
    $binary=base64_decode($base);
    header('Content-Type: bitmap; charset=utf-8');
    $file = fopen($filename, 'wb');
    fwrite($file, $binary);
    fclose($file);
    echo '1';
?>