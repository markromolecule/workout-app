<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header("Content-Type: application/json");

include('db_config.php');

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    
    // Get JSON input
    $data = json_decode(file_get_contents("php://input"), true);
    if (!$data) {
        echo json_encode(["success" => "failure", "message" => "Invalid JSON"]);
        exit();
    }

    $user_username = $data['user_username'] ?? '';
    $user_password = $data['user_password'] ?? '';

    // Validate input
    if (empty($user_username) || empty($user_password)) {
        echo json_encode(["success" => "failure", "message" => "All fields are required"]);
        exit();
    }

    // Check if user exists
    $stmt = $conn->prepare("SELECT * FROM auth WHERE user_username = ?");
    $stmt->bind_param("s", $user_username);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result->num_rows > 0) {
        $user = $result->fetch_assoc();
        
        // Verify password
        if (password_verify($user_password, $user['user_password'])) {
            echo json_encode(["success" => "success", "message" => "Login successful", "user_id" => $user['user_id']]);
        } else {
            echo json_encode(["success" => "failure", "message" => "Incorrect password"]);
        }
    } else {
        echo json_encode(["success" => "failure", "message" => "User not found"]);
    }

    $stmt->close();
    $conn->close();
} else {
    echo json_encode(["success" => "failure", "message" => "Invalid request method"]);
}
?>
