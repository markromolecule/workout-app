<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

include(__DIR__ . '/../db_config.php');

class LoginController {
    private $conn;

    public function __construct($db) {
        $this->conn = $db;
        if (!$this->conn) {
            $this->sendResponse(false, "Database connection failed", 500);
        }
    }

    public function handleRequest() {
        if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
            $this->sendResponse(false, "Invalid request method", 405);
        }

        $data = $this->getJsonInput();
        if (!$data) {
            $this->sendResponse(false, "Invalid JSON", 400);
        }

        $username = $data['user_username'] ?? '';
        $password = $data['user_password'] ?? '';

        if (empty($username) || empty($password)) {
            $this->sendResponse(false, "All fields are required", 400);
        }

        $this->processLogin($username, $password);
    }

    private function getJsonInput() {
        return json_decode(file_get_contents("php://input"), true);
    }

    private function sendResponse($success, $message, $statusCode = 200, $user = null) {
        http_response_code($statusCode); // Set HTTP status code
        $response = ["success" => $success, "message" => $message];
        if ($user) {
            $response["user"] = $user;
        }
        echo json_encode($response);
        exit();
    }

    private function processLogin($username, $password) {
        // Prepare the statement to prevent SQL injection
        $stmt = $this->conn->prepare("SELECT user_id, user_username, user_email, user_password FROM auth WHERE user_username = ?");
        $stmt->bind_param("s", $username);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows === 0) {
            $stmt->close();
            $this->sendResponse(false, "User not found", 404);
        }

        $user = $result->fetch_assoc();
        if (!password_verify($password, $user['user_password'])) {
            $stmt->close();
            $this->sendResponse(false, "Incorrect password", 401);
        }

        $userData = [
            "user_id" => $user['user_id'],
            "user_username" => $user['user_username'],
            "user_email" => $user['user_email']
        ];

        $stmt->close();
        $this->sendResponse(true, "Login successful", 200, $userData);
    }
}

// Initialize and handle request
$loginController = new LoginController($conn);
$loginController->handleRequest();
?>