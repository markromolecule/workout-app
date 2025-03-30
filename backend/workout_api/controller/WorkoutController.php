<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

include(__DIR__ . '/../db_config.php');

if (!$conn) {
    sendResponse(false, "Database connection failed");
}

if ($_SERVER["REQUEST_METHOD"] === "OPTIONS") {
    http_response_code(200);
    exit();
}

class WorkoutController
{
    private $conn;
    private $uploadDir;

    public function __construct($dbConnection)
    {
        $this->conn = $dbConnection;
        $this->uploadDir = realpath(__DIR__ . "/../uploads/") . "/";
        if (!is_dir($this->uploadDir) && !mkdir($this->uploadDir, 0777, true)) {
            sendResponse(false, "Failed to create upload directory");
        }
    }

    public function handleRequest()
    {
        error_log("DEBUG: Entered handleRequest()");

        $parsedUrl = parse_url($_SERVER["REQUEST_URI"], PHP_URL_PATH);
        $pathSegments = explode('/', trim($parsedUrl, '/'));

        $routineId = isset($_GET['routine_id']) ? intval($_GET['routine_id']) : null;

        if (!$routineId && isset($pathSegments[count($pathSegments) - 1]) && is_numeric($pathSegments[count($pathSegments) - 1])) {
            $routineId = intval($pathSegments[count($pathSegments) - 1]);
        }

        error_log("DEBUG: Extracted routineId = " . json_encode($routineId));

        switch ($_SERVER["REQUEST_METHOD"]) {
            case "POST":
                if (isset($_GET['progress'])) {
                    $this->routeToProgressController();
                } else {
                    $this->handleWorkoutUpload();
                }
                break;

            case "GET":
                $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
                $workoutID = isset($_GET['workout_id']) ? intval($_GET['workout_id']) : (isset($_GET['workoutID']) ? intval($_GET['workoutID']) : null);

                if ($workoutID !== null) {
                    $this->fetchWorkoutByID($workoutID, $user_id);
                } elseif ($user_id === null && $routineId === null) {
                    $this->fetchAllWorkouts();
                } else {
                    $this->fetchWorkouts($user_id, $routineId);
                }
                break;


            case "PUT":
                if (isset($_GET['progress'])) {
                    $this->routeToProgressController();
                } else {
                    $this->updateRoutineWorkout($routineId);
                }
                break;

            default:
                sendResponse(false, "Invalid request method");
        }
    }

    private function routeToProgressController()
    {
        include(__DIR__ . '/WorkoutProgressController.php');
        $controller = new WorkoutProgressController($this->conn);
        $controller->handleRequest();
    }

    private function handleWorkoutUpload()
    {
        // Check if a file is uploaded
        $fileKey = $_FILES['workoutImage'] ?? $_FILES['workoutImagePath'] ?? null;

        // If no file is uploaded, proceed without file upload logic
        if (!$fileKey) {
            // If no file, continue inserting workout data without the image
            $stmt = $this->conn->prepare("INSERT INTO workouts (workoutName, workoutDescription) VALUES (?, ?)");
            $stmt->bind_param("ss", $_POST['workoutName'], $_POST['workoutDescription']);
            $stmt->execute();
            $workoutID = $stmt->insert_id;
            $stmt->close();

            // Insert workout meta without image
            $this->insertWorkoutMeta($workoutID, 'workout_category', 'categoryName', $_POST['workoutCategory']);
            $this->insertWorkoutMeta($workoutID, 'workout_equipment', 'equipmentName', $_POST['workoutEquipment']);
            $this->insertWorkoutMeta($workoutID, 'workout_difficulty', 'difficultyLevel', $_POST['workoutDifficulty']);

            // Respond back with the success message
            sendResponse(true, "Workout added successfully without image");
            return;
        }

        // If a file is uploaded, process it
        $imageName = basename($fileKey["name"]);
        $relativePath = "uploads/" . $imageName;

        // Move the uploaded file to the desired directory
        move_uploaded_file($fileKey["tmp_name"], $this->uploadDir . $imageName);

        // Insert workout data with image path
        $stmt = $this->conn->prepare("INSERT INTO workouts (workoutName, workoutDescription, workoutImagePath) VALUES (?, ?, ?)");
        $stmt->bind_param("sss", $_POST['workoutName'], $_POST['workoutDescription'], $relativePath);
        $stmt->execute();
        $workoutID = $stmt->insert_id;
        $stmt->close();

        // Insert workout meta with image data
        $this->insertWorkoutMeta($workoutID, 'workout_category', 'categoryName', $_POST['workoutCategory']);
        $this->insertWorkoutMeta($workoutID, 'workout_equipment', 'equipmentName', $_POST['workoutEquipment']);
        $this->insertWorkoutMeta($workoutID, 'workout_difficulty', 'difficultyLevel', $_POST['workoutDifficulty']);

        // Respond back with the success message, including the image path
        sendResponse(true, "Workout added successfully", ["imagePath" => "http://10.0.2.2/workout_api/" . $relativePath]);
    }

    private function fetchWorkouts($user_id = null, $routine_id = null)
    {
        error_log("DEBUG: Inside fetchWorkouts function");

        // If no user_id & routine_id provided, fetch ALL workouts
        if ($user_id === null && $routine_id === null) {
            error_log("DEBUG: Fetching ALL workouts (no filters applied)");

            $query = "SELECT 
                        w.workoutID, 
                        w.workoutName, 
                        w.workoutDescription, 
                        CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                        COALESCE(wc.categoryName, 'Unknown') AS categoryName,
                        COALESCE(we.equipmentName, 'None') AS equipmentName,
                        COALESCE(wd.difficultyLevel, 'Unknown') AS workoutDifficulty
                      FROM workouts w
                      LEFT JOIN workout_category wc ON w.workoutID = wc.workoutID
                      LEFT JOIN workout_equipment we ON w.workoutID = we.workoutID
                      LEFT JOIN workout_difficulty wd ON w.workoutID = wd.workoutID";

            $stmt = $this->conn->prepare($query);
            if (!$stmt) {
                sendResponse(false, "Query preparation failed: " . $this->conn->error);
            }

            $stmt->execute();
            $result = $stmt->get_result();
            $workouts = [];
            while ($row = $result->fetch_assoc()) {
                $workouts[] = $row;
            }
            sendResponse(true, "All workouts retrieved successfully", $workouts);
        }

        if ($user_id === null || $routine_id === null) {
            sendResponse(false, "Missing required parameters: user_id and routine_id");
        }

        // Fetch workouts from `user_routine_workouts` first
        error_log("DEBUG: Fetching workouts for user_id=$user_id, routine_id=$routine_id");

        $query = "SELECT 
                    urw.user_routine_id,
                    urw.user_id,
                    urw.routine_id,
                    urw.workout_id AS workoutID,
                    urw.sets,
                    urw.reps,
                    urw.duration,
                    w.workoutName,
                    w.workoutDescription,
                    CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                    COALESCE(wc.categoryName, 'Unknown') AS categoryName,
                    COALESCE(we.equipmentName, 'None') AS equipmentName,
                    COALESCE(wd.difficultyLevel, 'Unknown') AS workoutDifficulty,
                    COALESCE((
                        SELECT CASE 
                            WHEN uwp.status = 'completed' THEN TRUE 
                            ELSE FALSE 
                        END 
                        FROM user_workout_progress uwp 
                        WHERE uwp.user_routine_id = urw.user_routine_id 
                        LIMIT 1
                    ), FALSE) AS isCompleted
                  FROM user_routine_workouts urw
                  INNER JOIN workouts w ON urw.workout_id = w.workoutID
                  LEFT JOIN workout_category wc ON w.workoutID = wc.workoutID
                  LEFT JOIN workout_equipment we ON w.workoutID = we.workoutID
                  LEFT JOIN workout_difficulty wd ON w.workoutID = wd.workoutID
                  WHERE urw.user_id = ? AND urw.routine_id = ?";

        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            sendResponse(false, "Query preparation failed: " . $this->conn->error);
        }

        $stmt->bind_param("ii", $user_id, $routine_id);
        $stmt->execute();
        $result = $stmt->get_result();
        $workouts = [];

        while ($row = $result->fetch_assoc()) {
            // Cast isCompleted to boolean
            $row['isCompleted'] = (bool) $row['isCompleted'];
            $workouts[] = $row;
        }

        if (empty($workouts)) {
            // Fetch from routine_workouts if no workouts in user_routine_workouts
            error_log("DEBUG: No workouts found in user_routine_workouts, fetching from routine_workouts...");

            $query = "SELECT 
                        rw.routineID,
                        rw.workoutID,
                        rw.sets, rw.reps, rw.duration,
                        w.workoutName,
                        w.workoutDescription,
                        CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                        COALESCE(wc.categoryName, 'Unknown') AS categoryName,
                        COALESCE(we.equipmentName, 'None') AS equipmentName,
                        COALESCE(wd.difficultyLevel, 'Unknown') AS workoutDifficulty,
                        FALSE AS isCompleted
                      FROM routine_workouts rw
                      INNER JOIN workouts w ON rw.workoutID = w.workoutID
                      LEFT JOIN workout_category wc ON w.workoutID = wc.workoutID
                      LEFT JOIN workout_equipment we ON w.workoutID = we.workoutID
                      LEFT JOIN workout_difficulty wd ON w.workoutID = wd.workoutID
                      WHERE rw.routineID = ?";

            $stmt = $this->conn->prepare($query);
            if (!$stmt) {
                sendResponse(false, "Query preparation failed: " . $this->conn->error);
            }

            $stmt->bind_param("i", $routine_id);
            $stmt->execute();
            $result = $stmt->get_result();

            while ($row = $result->fetch_assoc()) {
                // Ensure isCompleted is a boolean
                $row['isCompleted'] = (bool) $row['isCompleted'];
                $workouts[] = $row;
            }
        }

        if (empty($workouts)) {
            sendResponse(false, "No workouts found for this routine");
        } else {
            sendResponse(true, "Workouts retrieved successfully", $workouts);
        }
    }

    private function fetchAllWorkouts()
    {
        error_log("DEBUG: Fetching all workouts");

        $query = "SELECT 
                    w.workoutID, 
                    w.workoutName, 
                    w.workoutDescription, 
                    CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                    COALESCE(wc.categoryName, 'Unknown') AS categoryName,
                    COALESCE(we.equipmentName, 'None') AS equipmentName,
                    COALESCE(wd.difficultyLevel, 'Unknown') AS workoutDifficulty
                  FROM workouts w
                  LEFT JOIN workout_category wc ON w.workoutID = wc.workoutID
                  LEFT JOIN workout_equipment we ON w.workoutID = we.workoutID
                  LEFT JOIN workout_difficulty wd ON w.workoutID = wd.workoutID";

        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            sendResponse(false, "Query preparation failed: " . $this->conn->error);
        }

        $stmt->execute();
        $result = $stmt->get_result();
        $workouts = [];

        while ($row = $result->fetch_assoc()) {
            $workouts[] = $row;
        }

        sendResponse(true, "All workouts retrieved successfully", $workouts);
    }

    private function fetchWorkoutByID($workoutID, $user_id = null)
    {
        if (!$workoutID) {
            sendResponse(false, "Invalid workout ID");
        }

        $routine_id = isset($_GET['routine_id']) ? intval($_GET['routine_id']) : null;

        // SQL query to fetch the workout details
        $query = "SELECT 
                    w.workoutID, 
                    w.workoutName, 
                    w.workoutDescription, 
                    CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                    COALESCE(wc.categoryName, 'Unknown') AS workoutCategory, 
                    COALESCE(we.equipmentName, 'None') AS workoutEquipment, 
                    COALESCE(wd.difficultyLevel, 'Unknown') AS workoutDifficulty,
                    COALESCE(rw.sets, 0) AS sets, 
                    COALESCE(rw.reps, 0) AS reps, 
                    COALESCE(rw.duration, 30) AS duration";

        if ($routine_id !== null) {
            $query .= ", COALESCE((
                        SELECT CASE 
                            WHEN uwp.status = 'completed' THEN TRUE 
                            ELSE FALSE 
                        END 
                        FROM user_workout_progress uwp 
                        JOIN user_routine_workouts urw ON uwp.user_routine_id = urw.user_routine_id
                        WHERE urw.user_id = ? AND urw.workout_id = w.workoutID
                    ), FALSE) AS isCompleted";
        } else {
            $query .= ", FALSE AS isCompleted";
        }

        $query .= " FROM workouts w
                    LEFT JOIN workout_category wc ON w.workoutID = wc.workoutID
                    LEFT JOIN workout_equipment we ON w.workoutID = we.workoutID
                    LEFT JOIN workout_difficulty wd ON w.workoutID = wd.workoutID
                    LEFT JOIN routine_workouts rw ON w.workoutID = rw.workoutID
                    WHERE w.workoutID = ?
                    GROUP BY w.workoutID";

        // Prepare the SQL query
        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            sendResponse(false, "Failed to prepare statement", ["error" => $this->conn->error]);
        }

        // Bind parameters correctly
        if ($routine_id !== null) {
            $stmt->bind_param("ii", $user_id, $workoutID);
        } else {
            $stmt->bind_param("i", $workoutID);
        }

        // Execute the query
        $stmt->execute();
        $result = $stmt->get_result();

        // Check if any result was found
        if ($result->num_rows > 0) {
            $workout = $result->fetch_assoc();
            $workout['isCompleted'] = (bool) $workout['isCompleted'];
            sendResponse(true, "Workout fetched successfully", $workout);
        } else {
            sendResponse(false, "Workout not found");
        }
    }

    private function updateRoutineWorkout($routineId)
    {
        if (!$routineId) {
            sendResponse(false, "Missing or invalid routineId in URL.");
        }

        // Read the raw input (JSON body from the PUT request)
        $json = file_get_contents("php://input");
        error_log("DEBUG: Received JSON body = " . $json);
        $input = json_decode(trim($json), true);

        // Validation check for required fields
        if (!$input || !is_array($input) || count($input) === 0) {
            sendResponse(false, "Invalid request data: no workout data provided.");
            return;
        }

        $updatedWorkouts = [];
        $updatedCount = 0;
        $insertedCount = 0;

        // Process each workout in the input array
        foreach ($input as $workout) {
            // Ensure the workout_id and other required fields exist
            if (!isset($workout['user_id'], $workout['workout_id'], $workout['sets'], $workout['reps'], $workout['duration'])) {
                error_log("ERROR: Missing required fields for workout: " . print_r($workout, true));
                sendResponse(false, "Missing required fields for workout_id: " . (isset($workout['workout_id']) ? $workout['workout_id'] : 'N/A'));
                return;
            }

            $user_id = intval($workout['user_id']);
            $workout_id = intval($workout['workout_id']);
            $sets = intval($workout['sets']);
            $reps = intval($workout['reps']);
            $duration = intval($workout['duration']);

            // Validate that the workout exists in the workouts table
            $stmt = $this->conn->prepare("SELECT workoutID FROM workouts WHERE workoutID = ?");
            $stmt->bind_param("i", $workout_id);
            $stmt->execute();
            $result = $stmt->get_result();

            if ($result->num_rows === 0) {
                sendResponse(false, "Invalid workout_id: $workout_id does not exist in workouts table");
                return;
            }

            // Check if the workout exists in user_routine_workouts for the specified routine
            $stmt = $this->conn->prepare("SELECT user_routine_id FROM user_routine_workouts WHERE user_id = ? AND routine_id = ? AND workout_id = ?");
            $stmt->bind_param("iii", $user_id, $routineId, $workout_id);
            $stmt->execute();
            $result = $stmt->get_result();

            if ($result->num_rows > 0) {
                // Update existing workout
                $stmt = $this->conn->prepare("UPDATE user_routine_workouts SET sets = ?, reps = ?, duration = ? WHERE user_id = ? AND routine_id = ? AND workout_id = ?");
                $stmt->bind_param("iiiiii", $sets, $reps, $duration, $user_id, $routineId, $workout_id);
                $stmt->execute();
                if ($stmt->affected_rows > 0) {
                    $updatedCount++;
                    $updatedWorkouts[] = $this->fetchUpdatedWorkout($user_id, $routineId, $workout_id);
                }
            } else {
                // Insert new workout if not found
                $stmt = $this->conn->prepare("INSERT INTO user_routine_workouts (user_id, routine_id, workout_id, sets, reps, duration) VALUES (?, ?, ?, ?, ?, ?)");
                $stmt->bind_param("iiiiii", $user_id, $routineId, $workout_id, $sets, $reps, $duration);
                $stmt->execute();
                if ($stmt->affected_rows > 0) {
                    $insertedCount++;
                    $updatedWorkouts[] = $this->fetchUpdatedWorkout($user_id, $routineId, $workout_id);
                }
            }
        }

        sendResponse(true, "Routine workouts updated successfully", [
            "updated" => $updatedCount,
            "inserted" => $insertedCount,
            "workouts" => $updatedWorkouts
        ]);
    }

    private function fetchUpdatedWorkout($user_id, $routine_id, $workout_id)
    {
        $stmt = $this->conn->prepare("SELECT w.workoutID, w.workoutName, w.workoutDescription, 
                                            CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                                            COALESCE(rw.sets, 0) AS sets, 
                                            COALESCE(rw.reps, 0) AS reps, 
                                            COALESCE(rw.duration, 30) AS duration
                                      FROM workouts w
                                      LEFT JOIN user_routine_workouts rw ON w.workoutID = rw.workout_id
                                      WHERE w.workoutID = ? AND rw.user_id = ? AND rw.routine_id = ?");
        $stmt->bind_param("iii", $workout_id, $user_id, $routine_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows > 0) {
            return $result->fetch_assoc();
        } else {
            return ["error" => "Workout not found after update"];
        }
    }

    private function insertWorkoutMeta($workoutID, $table, $column, $value)
    {
        $stmt = $this->conn->prepare("INSERT INTO $table (workoutID, $column) VALUES (?, ?)");
        $stmt->bind_param("is", $workoutID, $value);
        $stmt->execute();
        $stmt->close();
    }
}
function sendResponse($success, $message, $data = [])
{
    http_response_code($success ? 200 : 400);
    echo json_encode([
        "success" => $success,
        "message" => $message,
        "data" => $data
    ]);
    exit();
}

$controller = new WorkoutController($conn);
$controller->handleRequest();