<?php
        include ("utility.php");
	$dayValue = $_GET['d'];
	$solarValue = $_GET['s'];
	$consValue = $_GET['c'];
	$lightValue = $_GET['l'];

	try {
		$dbh = new PDO('mysql:host=localhost;dbname=giesecke_sp',  $username, $password)or die("cannot open database");
		$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

		/* $query = "INSERT INTO s (d, s, c, l) VALUES('$dayValue','$solarValue','$consValue','$lightValue')";*/
		$query = "INSERT INTO s (d, s, c, l) SELECT * FROM (SELECT '$dayValue','$solarValue','$consValue','$lightValue') AS tmp WHERE NOT EXISTS ( SELECT d FROM s WHERE d = '$dayValue' ) LIMIT 1";
		$sth = $dbh->query($query);
		echo $sth->errorCode();
		echo "\n";
		$arr = $sth->errorInfo();
		print_r($arr);
	} catch(PDOException $e) {
		echo 'ERROR: ' . $e->getMessage();
	}
?>
