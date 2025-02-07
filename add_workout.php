<?php

include('db_config.php');
error_reporting(E_ALL);
ini_set('display_errors', 1);
header("Content-Type: application/json");

$response = [];

if ($_SERVER["REQUEST_METHOD"] === "POST") {
    // Debugging: Check if files are uploaded
    if (!isset($_FILES['workoutImagePath']) || $_FILES['workoutImagePath']['error'] !== UPLOAD_ERR_OK) {
        echo json_encode(["message" => "No image file uploaded or upload error"]);
        http_response_code(400);
        exit();
    }

    $upload_dir = "uploads/";

    // Ensure upload directory exists
    if (!is_dir($upload_dir) && !mkdir($upload_dir, 0777, true)) {
        echo json_encode(["message" => "Failed to create upload directory"]);
        http_response_code(500);
        exit();
    }

    // Generate a unique filename
    $image_name = basename($_FILES["workoutImagePath"]["name"]);
    $target_file = $upload_dir . time() . "_" . $image_name;

    // Move file and check if successful
    if (!move_uploaded_file($_FILES["workoutImagePath"]["tmp_name"], $target_file)) {
        echo json_encode(["message" => "Failed to move uploaded file"]);
        http_response_code(500);
        exit();
    }

    // Collect POST data
    $workoutName = $_POST['workoutName'] ?? '';
    $workoutCategory = $_POST['workoutCategory'] ?? '';
    $workoutDescription = $_POST['workoutDescription'] ?? '';
    $workoutDifficulty = $_POST['workoutDifficulty'] ?? '';
    $workoutEquipment = $_POST['workoutEquipment'] ?? '';
    $workoutImagePath = $target_file;

    // Validate required fields
    if (!$workoutName || !$workoutCategory || !$workoutDescription || !$workoutDifficulty || !$workoutEquipment) {
        echo json_encode(["message" => "All fields are required"]);
        http_response_code(400);
        exit();
    }

    // Insert into database
    $stmt = $conn->prepare("INSERT INTO workouts (workoutName, workoutCategory, workoutDescription, workoutImagePath, workoutDifficulty, workoutEquipment) VALUES (?, ?, ?, ?, ?, ?)");
    $stmt->bind_param("ssssss", $workoutName, $workoutCategory, $workoutDescription, $workoutImagePath, $workoutDifficulty, $workoutEquipment);

    if ($stmt->execute()) {
        $response["message"] = "Workout added successfully";
        $response["imagePath"] = $workoutImagePath;
        http_response_code(200);
    } else {
        $response["message"] = "Database error: " . $stmt->error;
        http_response_code(500);
    }

    $stmt->close();
    $conn->close();
} else {
    $response["message"] = "Invalid request method";
    http_response_code(405);
}

echo json_encode($response);

?>