<?php
$response["images"] = array();
	

$dir = opendir('uploads');
while ($file = readdir($dir)) {
	$images = array();
    if ($file == '.' || $file == '..') {
        continue;
    }

    $images["url"] = "http://npoolshop.com/images/uploads/".$file;
	
	array_push($response["images"], $images);
}

header('Content-type: application/json');
echo json_encode($response);

?>