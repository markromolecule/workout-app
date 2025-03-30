<?php
include(__DIR__ . '/../db_config.php');

error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

class UserController {

    private $conn;

    public function __construct($db) {
        $this->conn = $db;
    }

    // Handle the request based on the HTTP method
    public function handleRequest() {
        switch ($_SERVER["REQUEST_METHOD"]) {
            case "POST":
                $input = json_decode(file_get_contents("php://input"), true) ?? $_POST;
                if (isset($input['username']) && isset($input['password'])) {
                    $this->processLogin($input['username'], $input['password']);
                } elseif (isset($input['user_id']) && is_numeric($input['user_id'])) {
                    $this->processProfileUpdate($input);
                } else {
                    sendResponse(false, "Invalid request");
                }
                break;

            case "PUT":
                $input = json_decode(file_get_contents("php://input"), true) ?? $_POST;
                if (isset($input['user_id']) && is_numeric($input['user_id'])) {
                    $this->processProfileUpdate($input);
                } else {
                    sendResponse(false, "Invalid request");
                }
                break;

            case "GET":
                $this->handleGetRequest();
                break;

            default:
                sendResponse(false, "Invalid request method");
        }
    }

    // Handle GET request - retrieve user profile
    public function handleGetRequest(): void {
        if (!isset($_GET['id'])) {
            sendResponse(false, "Missing user ID");
            return;
        }

        $userId = intval($_GET['id']);
        $query = "SELECT a.user_fullname, a.user_username, a.user_email,
                 h.height_cm, h.weight_kg, 
                 p.gender, p.userProfile
          FROM auth a
          LEFT JOIN user_health h ON a.user_id = h.user_id
          LEFT JOIN user_profile p ON a.user_id = p.user_id
          WHERE a.user_id = ?";

        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            sendResponse(false, "Database error: " . $this->conn->error);
            return;
        }

        $stmt->bind_param("i", $userId);
        $stmt->execute();
        $stmt->store_result();

        // Declare variables before binding results
        $fullname = $username = $email = $height = $weight = $gender = $profilePath = null;

        $stmt->bind_result($fullname, $username, $email, $height, $weight, $gender, $profilePath);

        if ($stmt->fetch()) {
            sendResponse(true, "User found", [
                "user_fullname" => $fullname,
                "user_username" => $username,
                "user_email" => $email,
                "height_cm" => $height !== null ? number_format((float) $height, 2, '.', '') : null,
                "weight_kg" => $weight !== null ? number_format((float) $weight, 2, '.', '') : null,
                "gender" => $gender,
                "userProfile" => $profilePath ? "http://10.0.2.2/workout_api/" . $profilePath : null
            ]);
        } else {
            sendResponse(false, "User not found");
        }
        $stmt->close();
    }

    // Handle login - verifies credentials and logs in the user
    public function processLogin($username, $password): void {
        error_log("Attempting login for username: " . $username);

        $stmt = $this->conn->prepare("SELECT user_id, user_username, user_email, user_password FROM auth WHERE user_username = ?");
        if (!$stmt) {
            sendResponse(false, "Database error: " . $this->conn->error);
            return;
        }

        $stmt->bind_param("s", $username);
        $stmt->execute();
        $stmt->store_result();

        // Declare variables before binding results
        $userId = $dbUsername = $email = $hashedPassword = null;

        if ($stmt->num_rows > 0) {
            $stmt->bind_result($userId, $dbUsername, $email, $hashedPassword);
            $stmt->fetch();

            // Ensure $hashedPassword is not null before verifying
            if ($hashedPassword !== null && password_verify($password, $hashedPassword)) {
                sendResponse(true, "Login successful", [
                    "user" => [
                        "user_id" => $userId,
                        "user_username" => $dbUsername,
                        "user_email" => $email
                    ]
                ]);
            } else {
                sendResponse(false, "Invalid username or password");
            }
        } else {
            sendResponse(false, "Invalid username or password");
        }

        $stmt->close();
    }

    // Handle profile update
    public function processProfileUpdate($input): void {
        $userId = intval($input['user_id']);
        $fullName = $input['user_fullname'] ?? null;
        $username = $input['user_username'] ?? null;
        $email = $input['user_email'] ?? null;
        $height = isset($input['height_cm']) ? floatval($input['height_cm']) : null;
        $weight = isset($input['weight_kg']) ? floatval($input['weight_kg']) : null;
        $gender = $input['gender'] ?? null;
    
        if (!$userId) {
            sendResponse(false, "Missing user ID");
            return;
        }
    
        // Update user data in `auth` table
        $stmt = $this->conn->prepare("UPDATE auth 
                                      SET user_fullname = COALESCE(?, user_fullname),
                                          user_username = COALESCE(?, user_username),
                                          user_email = COALESCE(?, user_email)
                                      WHERE user_id = ?");
        if (!$stmt) {
            sendResponse(false, "Database error: " . $this->conn->error);
            return;
        }
    
        $stmt->bind_param("sssi", $fullName, $username, $email, $userId);
        if (!$stmt->execute()) {
            sendResponse(false, "Update failed - " . $stmt->error);
            return;
        }
    
        // Update height and weight in `user_health` table
        if ($height !== null || $weight !== null) {
            $stmt = $this->conn->prepare("INSERT INTO user_health (user_id, height_cm, weight_kg)
                                          VALUES (?, ?, ?)
                                          ON DUPLICATE KEY UPDATE 
                                          height_cm = COALESCE(?, height_cm),
                                          weight_kg = COALESCE(?, weight_kg)");
            if (!$stmt) {
                sendResponse(false, "Database error: " . $this->conn->error);
                return;
            }
    
            // Correct the bind_param to only use 5 parameters
            $stmt->bind_param("iddds", $userId, $height, $weight, $height, $weight);
            if (!$stmt->execute()) {
                sendResponse(false, "Health data update failed - " . $stmt->error);
                return;
            }
        }
    
        // Update gender in `user_profile` table
        if ($gender !== null) {
            $stmt = $this->conn->prepare("INSERT INTO user_profile (user_id, gender)
                                          VALUES (?, ?)
                                          ON DUPLICATE KEY UPDATE 
                                          gender = COALESCE(?, gender)");
            if (!$stmt) {
                sendResponse(false, "Database error: " . $this->conn->error);
                return;
            }
    
            $stmt->bind_param("iss", $userId, $gender, $gender);
            if (!$stmt->execute()) {
                sendResponse(false, "Profile data update failed - " . $stmt->error);
                return;
            }
        }
    
        sendResponse(true, "Profile updated successfully");
    }
}

// Utility function to send JSON responses
function sendResponse($success, $message, $data = []) {
    echo json_encode([
        "success" => $success,
        "message" => $message,
        "data" => $data
    ]);
}

// Initialize controller
$controller = new UserController($conn);

// Handle request
$controller->handleRequest();
?>