<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

include(__DIR__ . '/../db_config.php');

class SignupController {
    private $conn;

    public function __construct($db) {
        $this->conn = $db;
        if (!$this->conn) {
            $this->sendResponse(false, "Database connection failed", 500);
        }
    }

    public function handleRequest() {
        if ($_SERVER["REQUEST_METHOD"] !== "POST") {
            $this->sendResponse(false, "Invalid request method", 405);
        }

        $data = $this->getJsonInput();
        if (!$data) {
            $this->sendResponse(false, "Invalid JSON", 400);
        }

        // Validate input fields
        $fullname = $this->sanitizeInput($data['user_fullname'] ?? '');
        $username = $this->sanitizeInput($data['user_username'] ?? '');
        $email = filter_var($data['user_email'] ?? '', FILTER_VALIDATE_EMAIL);
        $password = $data['user_password'] ?? '';

        if (empty($fullname) || empty($username) || !$email || empty($password)) {
            $this->sendResponse(false, "All fields are required and email must be valid", 400);
        }

        $hashedPassword = password_hash($password, PASSWORD_DEFAULT);

        $this->checkUserExists($username, $email);
        $this->registerUser($fullname, $username, $email, $hashedPassword);
    }

    private function getJsonInput() {
        return json_decode(file_get_contents("php://input"), true);
    }

    private function sanitizeInput($input) {
        // Using FILTER_SANITIZE_FULL_SPECIAL_CHARS instead of FILTER_SANITIZE_STRING
        return filter_var(trim($input), FILTER_SANITIZE_FULL_SPECIAL_CHARS);
    }

    private function sendResponse($success, $message, $statusCode = 200) {
        http_response_code($statusCode);
        echo json_encode(["success" => $success, "message" => $message]);
        exit();
    }

    private function checkUserExists($username, $email) {
        $query = "SELECT user_id FROM auth WHERE user_username = ? OR user_email = ?";
        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("ss", $username, $email);
        $stmt->execute();
        $stmt->store_result();

        if ($stmt->num_rows > 0) {
            $stmt->close();
            $this->sendResponse(false, "Username or email already exists", 409);
        }
        $stmt->close();
    }

    private function registerUser($fullname, $username, $email, $password) {
        $query = "INSERT INTO auth (user_fullname, user_username, user_email, user_password) VALUES (?, ?, ?, ?)";
        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("ssss", $fullname, $username, $email, $password);

        if ($stmt->execute()) {
            $stmt->close();
            $this->sendResponse(true, "User registered successfully", 201);
        } else {
            $stmt->close();
            $this->sendResponse(false, "Failed to register user", 500);
        }
    }
}

// Initialize and handle request
$signupController = new SignupController($conn);
$signupController->handleRequest();
?>