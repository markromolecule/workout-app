
<?php
$servername = "localhost";
$username = "root";
$password = ""; 
$dbname = "workout_db"; 
$port = 3307;

// Create connection
$conn = new mysqli($servername, $username, 
                    $password, $dbname, $port);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}
?>