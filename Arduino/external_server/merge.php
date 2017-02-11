<?php
	include ("utility.php");

	/* We started recording in August 2015 */
	$year = 15;
	$month = 8;

	/* Open MySQL database */
	$dbh = new PDO('mysql:host=localhost;dbname=giesecke_sp',  $username, $password)or die("cannot open database");
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	while (1) {
		$sqlDBname = './sq-db/' . $year . '-';
		if ($month < 10) {
			$sqlDBname = $sqlDBname . '0';
		}
		$sqlDBname = $sqlDBname . $month . '.db';
		
		if (file_exists($sqlDBname)) {
			echo "Processing: " . $sqlDBname . "\n";
		} else {
			echo "File does not exist ==> exit()\n";
			exit(0);
		}
		
		/* Open SqLite3 database */
		$pdoString = 'sqlite:' . $sqlDBname;
		
		$file_db = new PDO($pdoString);
		$file_db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		
		/* Get all entries from SqLite3 database */
		$result = $file_db->query("SELECT * FROM s");
		foreach ($result as $m) {
			/* Get values of each entry one by one */
			$dayValue = $m['d'];
			$solarValue = $m['s'];
			$consValue = $m['c'];
			$lightValue = $m['l'];
			// echo 'Day: ' . $dayValue . ' S: ' . $solarValue . ' C: ' . $consValue . "\n";
			
			try {
				/* Insert into MySQL database if entry does not exist */
				$query = "INSERT INTO s (d, s, c, l) SELECT * FROM (SELECT '$dayValue','$solarValue','$consValue','$lightValue') AS tmp WHERE NOT EXISTS ( SELECT d FROM s WHERE d = '$dayValue' ) LIMIT 1";
				$sth = $dbh->query($query);
				if ($sth->errorCode() != 00000) {
					echo $sth->errorCode();
					echo "\n";
				}
			} catch(PDOException $e) {
				echo 'ERROR: ' . $e->getMessage();
			}

		}
		$month = $month + 1;
		if ($month == 13) {
			$month = 1;
			$year = $year + 1;
		}
		$file_db = null;
	}
	$dbh = null;
?>
