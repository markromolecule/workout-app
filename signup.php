<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header("Content-Type: application/json");

include('db_config.php');

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $data = json_decode(file_get_contents("php://input"), true);

    if (!isset($data['user_fullname']) || !isset($data['user_username']) || !isset($data['user_email']) || !isset($data['user_password'])) {
        echo json_encode(['success' => false, 'message' => 'All fields are required']);
        exit();
    }

    $user_fullname = $data['user_fullname'];
    $user_username = $data['user_username'];
    $user_email = $data['user_email'];
    $user_password = $data['user_password'];

    if (empty($user_fullname) || empty($user_username) || empty($user_email) || empty($user_password)) {
        echo json_encode(['success' => false, 'message' => 'All fields are required']);
        exit();
    }

    $password_hashed = password_hash($user_password, PASSWORD_DEFAULT);

   // Check if username or email already exists
    $query = "SELECT user_id FROM auth WHERE user_username = ? OR user_email = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("ss", $user_username, $user_email);
    $stmt->execute();
    $stmt->store_result();

    if ($stmt->num_rows > 0) {
        http_response_code(409);
        echo json_encode(['success' => false, 'message' => 'Username or email already exists']);
        exit();
    }
    $stmt->close();

    // Insert user into database
    $query = "INSERT INTO auth (user_fullname, user_username, user_email, user_password) VALUES (?, ?, ?, ?)";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("ssss", $user_fullname, $user_username, $user_email, $password_hashed);

    if ($stmt->execute()) {
        $response = ['success' => 'success', 'message' => 'User registered successfully'];
        error_log(json_encode($response)); 
        echo json_encode($response);
    } else {
        $response = ['success' => 'failure', 'message' => 'Failed to register user'];
        error_log(json_encode($response)); 
        echo json_encode($response);
    }
    

    $stmt->close();
    $conn->close();
} else {
    echo json_encode(['success' => false, 'message' => 'Invalid request method']);
}
?>