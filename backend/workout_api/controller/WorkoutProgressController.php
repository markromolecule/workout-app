<?php
include(__DIR__ . '/../db_config.php');

error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

class WorkoutProgressController
{
    private $conn;

    public function __construct($dbConnection)
    {
        $this->conn = $dbConnection;
    }

    public function handleRequest()
    {
        $method = $_SERVER["REQUEST_METHOD"];
        // We don't need 'routine_id' in the 'GET' request for in-progress routines
        $data = ($method == 'GET') ? $_GET : $this->getRequestData(['user_id', 'routine_id']); // Adjust this logic

        switch ($method) {
            case "POST":
                if (isset($data['workout_id'])) {
                    $this->startWorkoutProgress();
                } else {
                    $this->startRoutineProgress();
                }
                break;

            case "PUT":
                if (isset($data['workout_id'])) {
                    $this->completeWorkoutProgress();
                } else {
                    $this->completeRoutineProgress();
                }
                break;

            case "GET":
                if (isset($data['user_id']) && isset($data['routine_id'])) {
                    // Handle GET for workout progress with user_id and routine_id
                    $this->getWorkoutProgress(intval($data['user_id']), intval($data['routine_id']));
                } elseif (isset($data['routine_id'])) {
                    // Handle GET for specific routine workouts
                    $this->getWorkoutsForRoutine(intval($data['routine_id']));
                } elseif (isset($data['user_id']) && isset($data['completed'])) {
                    // Handle GET for completed routines
                    $this->getCompletedRoutines(intval($data['user_id']));
                } elseif (isset($data['user_id'])) {
                    // Handle GET for in-progress routines with just user_id
                    $this->getInProgressRoutines(intval($data['user_id']));
                } else {
                    $this->sendResponse(400, ["message" => "Missing required parameters"]);
                }
                break;

            default:
                $this->sendResponse(405, ["message" => "Invalid request method"]);
        }
    }

    public function getCompletedRoutines($user_id)
    {
        if (!$user_id) {
            $this->sendResponse(400, ["message" => "User ID is required"]);
        }

        $query = "SELECT 
                 r.routineID, 
                 r.routineName, 
                 r.routineDescription, 
                 r.routineDuration, 
                 r.routineDifficulty, 
                 COUNT(rw.workoutID) AS exerciseCount,
                 1 AS isCompleted  -- Force isCompleted = 1 since we are filtering only completed routines
              FROM routines r
              JOIN routine_workouts rw ON r.routineID = rw.routineID
              JOIN user_routine_progress urp ON r.routineID = urp.routine_id 
              JOIN routine_status rs ON urp.routine_progress_id = rs.routine_progress_id
              WHERE urp.user_id = ? AND rs.status = 'completed'  -- ✅ Filtering only completed routines
              GROUP BY r.routineID";

        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $completedRoutines = [];
        while ($row = $result->fetch_assoc()) {
            $completedRoutines[] = $row;
        }

        if (empty($completedRoutines)) {
            $this->sendResponse(200, ["success" => true, "data" => [], "message" => "No completed routines found"]);
        } else {
            $this->sendResponse(200, ["success" => true, "data" => $completedRoutines]);
        }
    }

    private function getInProgressRoutines($user_id)
    {
        $query = "SELECT urp.routine_id, r.routineName 
          FROM user_routine_progress urp
          JOIN routine_status rs ON urp.routine_progress_id = rs.routine_progress_id
          JOIN routines r ON urp.routine_id = r.routineID
          WHERE urp.user_id = ? AND rs.status = 'in-progress'";

        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $routines = [];
        while ($row = $result->fetch_assoc()) {
            $routines[] = [
                'routine_id' => intval($row['routine_id']),
                'routineName' => $row['routineName']
            ];
        }

        if (empty($routines)) {
            $this->sendResponse(404, ["message" => "No in-progress routines found"]);
        } else {
            $this->sendResponse(200, ["success" => true, "data" => $routines]);
        }
    }

    // Start routine progress
    private function startRoutineProgress()
    {
        $data = $this->getRequestData(['user_id', 'routine_id']);
        $user_id = intval($data['user_id']);
        $routine_id = intval($data['routine_id']);

        $routine_progress_id = $this->getRoutineProgressID($user_id, $routine_id) ?? $this->insertRoutineProgress($user_id, $routine_id);
        $this->insertRoutineStatus($routine_progress_id);

        $this->sendResponse(200, ["success" => true, "message" => "Routine progress started", "routine_progress_id" => $routine_progress_id]);
    }

    private function completeRoutineProgress()
    {
        $data = $this->getRequestData(['user_id', 'routine_id']);
        $user_id = intval($data['user_id']);
        $routine_id = intval($data['routine_id']);

        // Get routine progress ID
        $routine_progress_id = $this->getRoutineProgressID($user_id, $routine_id);
        if (!$routine_progress_id) {
            error_log("ERROR: Routine progress not found for user_id=$user_id, routine_id=$routine_id");
            $this->sendResponse(404, ["message" => "Routine progress not found"]);
        }

        // Check if all workouts are completed
        $stmt = $this->conn->prepare("
        SELECT COUNT(*) FROM user_workout_progress 
        WHERE user_routine_id IN (
            SELECT user_routine_id FROM user_routine_workouts WHERE routine_id = ?
        ) AND status != 'completed'
    ");
        $stmt->bind_param("i", $routine_id);
        $stmt->execute();
        $result = $stmt->get_result();
        $row = $result->fetch_row();
        $incomplete_workouts_count = $row[0];

        error_log("DEBUG: Incomplete workouts count for routine_id=$routine_id -> " . $incomplete_workouts_count);

        if ($incomplete_workouts_count > 0) {
            error_log("ERROR: Cannot complete routine because $incomplete_workouts_count workouts are still in progress.");
            $this->sendResponse(400, ["message" => "All workouts must be completed before completing the routine."]);
        }

        // Start database transaction
        $this->conn->autocommit(false);

        try {
            // Check if routine_status exists
            $stmtCheck = $this->conn->prepare("
            SELECT status FROM routine_status WHERE routine_progress_id = ?
        ");
            $stmtCheck->bind_param("i", $routine_progress_id);
            $stmtCheck->execute();
            $stmtCheck->store_result();

            if ($stmtCheck->num_rows === 0) {
                error_log("DEBUG: No existing routine_status found. Inserting new 'completed' status.");
                // Insert a new routine_status entry if it doesn't exist
                $stmtInsert = $this->conn->prepare("
                INSERT INTO routine_status (routine_progress_id, status, completion_time) 
                VALUES (?, 'completed', NOW())
            ");
                $stmtInsert->bind_param("i", $routine_progress_id);
                if (!$stmtInsert->execute()) {
                    throw new Exception("Failed to insert into routine_status: " . $stmtInsert->error);
                }
                $stmtInsert->close();
            } else {
                error_log("DEBUG: Existing routine_status found. Updating to 'completed'.");
                // Update routine_status if it exists
                $stmtRoutineStatus = $this->conn->prepare("
                UPDATE routine_status 
                SET status = 'completed', completion_time = NOW() 
                WHERE routine_progress_id = ?
            ");
                $stmtRoutineStatus->bind_param("i", $routine_progress_id);
                if (!$stmtRoutineStatus->execute()) {
                    throw new Exception("Failed to update routine_status: " . $stmtRoutineStatus->error);
                }
                $stmtRoutineStatus->close();
            }
            $stmtCheck->close();

            // Update `completion_time` in `user_routine_progress`
            error_log("DEBUG: Updating user_routine_progress completion_time for routine_progress_id=$routine_progress_id.");
            $stmtUserRoutine = $this->conn->prepare("
            UPDATE user_routine_progress 
            SET completion_time = NOW() 
            WHERE routine_progress_id = ?
        ");
            $stmtUserRoutine->bind_param("i", $routine_progress_id);
            if (!$stmtUserRoutine->execute()) {
                throw new Exception("Failed to update user_routine_progress completion_time: " . $stmtUserRoutine->error);
            }
            $stmtUserRoutine->close();

            // Commit transaction
            $this->conn->commit();
            $this->conn->autocommit(true);

            error_log("SUCCESS: Routine successfully marked as completed for routine_progress_id=$routine_progress_id.");
            $this->sendResponse(200, ["success" => true, "message" => "Routine marked as completed"]);

        } catch (Exception $e) {
            // Rollback if any failure occurs
            $this->conn->rollback();
            $this->conn->autocommit(true);
            error_log("ERROR: " . $e->getMessage());
            $this->sendResponse(500, ["message" => "Failed to complete routine", "error" => $e->getMessage()]);
        }
    }


    // Start workout progress
    private function startWorkoutProgress()
    {
        $data = $this->getRequestData(['user_id', 'routine_id', 'workout_id']);
        $user_id = intval($data['user_id']);
        $routine_id = intval($data['routine_id']);
        $workout_id = intval($data['workout_id']);

        $routine_progress_id = $this->getRoutineProgressID($user_id, $routine_id);
        if (!$routine_progress_id) {
            $this->sendResponse(404, ["message" => "Routine progress not found"]);
        }

        // Check if workout is already in-progress
        $stmt = $this->conn->prepare("SELECT progress_id FROM user_workout_progress 
                                  WHERE user_routine_id = ? AND workout_id = ? AND status = 'in-progress'");
        $stmt->bind_param("ii", $routine_progress_id, $workout_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($row = $result->fetch_assoc()) {
            error_log("DEBUG: Workout already in progress, progress_id = " . $row['progress_id']);
        } else {
            // Now, it's only executed when no "in-progress" workout exists
            $this->insertWorkoutProgress($user_id, $routine_progress_id, $workout_id);
            $this->sendResponse(200, ["success" => true, "message" => "Workout progress started"]);
        }
    }

    // Complete workout progress
    private function completeWorkoutProgress()
    {
        $data = $this->getRequestData(['user_id', 'routine_id', 'workout_id']);
        $user_id = intval($data['user_id']);
        $routine_id = intval($data['routine_id']);
        $workout_id = intval($data['workout_id']);

        error_log("DEBUG: Completing workout progress for user_id = $user_id, routine_id = $routine_id, workout_id = $workout_id");

        // Get routine progress ID
        $routine_progress_id = $this->getRoutineProgressID($user_id, $routine_id);
        if (!$routine_progress_id) {
            error_log("ERROR: Routine progress not found for user_id = $user_id, routine_id = $routine_id");
            $this->sendResponse(404, ["message" => "Routine progress not found"]);
        }

        // Find latest "in-progress" workout
        $stmt = $this->conn->prepare("SELECT progress_id FROM user_workout_progress 
                                  WHERE user_routine_id = ? AND workout_id = ? AND status = 'in-progress'
                                  ORDER BY start_time DESC LIMIT 1");
        if (!$stmt) {
            error_log("ERROR: Failed to prepare statement - " . $this->conn->error);
            $this->sendResponse(500, ["message" => "Database error", "error" => $this->conn->error]);
        }

        $stmt->bind_param("ii", $routine_progress_id, $workout_id);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($row = $result->fetch_assoc()) {
            $progress_id = $row['progress_id'];
            error_log("DEBUG: Found in-progress workout with progress_id: " . $progress_id);

            // Update workout status to 'completed' and set `completion_time`
            $updateStmt = $this->conn->prepare("UPDATE user_workout_progress 
                                            SET status = 'completed', completion_time = NOW() 
                                            WHERE progress_id = ?");
            if (!$updateStmt) {
                error_log("ERROR: Failed to prepare update statement - " . $this->conn->error);
                $this->sendResponse(500, ["message" => "Database error", "error" => $this->conn->error]);
            }

            $updateStmt->bind_param("i", $progress_id);
            $updateStmt->execute();

            if ($updateStmt->affected_rows > 0) {
                error_log("DEBUG: Workout completion updated successfully in database.");
                $this->sendResponse(200, ["success" => true, "message" => "Workout marked as completed"]);
            } else {
                error_log("ERROR: No rows affected. Workout progress not updated.");
                $this->sendResponse(500, ["message" => "Failed to update workout progress", "error" => $updateStmt->error]);
            }

            $updateStmt->close();
        } else {
            error_log("DEBUG: No 'in-progress' workout found for workout_id = " . $workout_id);
            $this->sendResponse(404, ["message" => "No 'in-progress' workout found to update"]);
        }

        $stmt->close();
    }

    // Insert workout progress
    private function insertWorkoutProgress($user_id, $routine_progress_id, $workout_id)
    {
        $stmt = $this->conn->prepare("INSERT INTO user_workout_progress 
                                  (user_id, user_routine_id, workout_id, status, start_time) 
                                  VALUES (?, ?, ?, 'in-progress', NOW())");
        $stmt->bind_param("iii", $user_id, $routine_progress_id, $workout_id);
        if (!$stmt->execute()) {
            $this->sendResponse(500, ["message" => "Failed to insert workout progress", "error" => $stmt->error]);
        }
    }
    private function getWorkoutsForRoutine($routine_id)
    {
        $stmt = $this->conn->prepare("
        SELECT urw.user_routine_id, urw.user_id, urw.routine_id, 
               w.workoutID,
               urw.sets, urw.reps, urw.duration, 
               w.workoutName, w.workoutDescription, 
               CONCAT('http://127.0.0.1/workout_api/', w.workoutImagePath) AS workoutImageURL, 
               wc.categoryName, we.equipmentName, wd.difficultyLevel AS workoutDifficulty, 
               FALSE AS isCompleted
        FROM user_routine_workouts urw
        JOIN workouts w ON urw.workout_id = w.workoutID
        LEFT JOIN workout_category wc ON w.workoutID = wc.workoutID
        LEFT JOIN workout_equipment we ON w.workoutID = we.workoutID
        LEFT JOIN workout_difficulty wd ON w.workoutID = wd.workoutID
        WHERE urw.routine_id = ?");

        $stmt->bind_param("i", $routine_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $workouts = [];
        while ($row = $result->fetch_assoc()) {
            $workouts[] = [
                "user_routine_id" => intval($row["user_routine_id"]),
                "user_id" => intval($row["user_id"]),
                "routine_id" => intval($row["routine_id"]),
                "workoutID" => intval($row["workoutID"]),
                "sets" => intval($row["sets"]),
                "reps" => intval($row["reps"]),
                "duration" => intval($row["duration"]),
                "workoutName" => $row["workoutName"],
                "workoutDescription" => $row["workoutDescription"],
                "workoutImageURL" => $row["workoutImageURL"],
                "categoryName" => $row["categoryName"],
                "equipmentName" => $row["equipmentName"],
                "workoutDifficulty" => $row["workoutDifficulty"],
                "isCompleted" => boolval($row["isCompleted"])
            ];
        }

        if (empty($workouts)) {
            $this->sendResponse(404, ["message" => "No workouts found for routine"]);
        } else {
            $this->sendResponse(200, ["success" => true, "data" => $workouts]);
        }
    }

    // Fetch user workout progress

    private function getWorkoutProgress($user_id, $routine_id)
    {
        $query = "SELECT 
                urp.routine_progress_id, 
                urp.user_id, 
                urp.routine_id, 
                rs.status, 
                urp.start_time, 
                rs.completion_time 
              FROM user_routine_progress urp
              LEFT JOIN routine_status rs ON urp.routine_progress_id = rs.routine_progress_id
              WHERE urp.user_id = ? AND urp.routine_id = ?";

        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("ii", $user_id, $routine_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $progress = [];
        while ($row = $result->fetch_assoc()) {
            $progress_entry = $row;

            // Ensure getWorkoutsForProgress() always returns an array
            $progress_entry["workouts"] = $this->getWorkoutsForProgress($row["routine_progress_id"]) ?? [];

            $progress[] = $progress_entry;
        }

        if (empty($progress)) {
            $this->sendResponse(404, ["message" => "No progress found for the user"]);
        } else {
            $this->sendResponse(200, ["success" => true, "progress" => $progress]);
        }
    }
    // Fetch workouts directly from `workouts` table
    private function getWorkoutsForProgress($routine_progress_id)
    {
        $query = "SELECT 
                uwp.progress_id, 
                uwp.user_routine_id, 
                uwp.workout_id AS workoutID,  
                uwp.status, 
                uwp.start_time, 
                uwp.completion_time, 
                w.workoutName, 
                w.workoutDescription, 
                w.workoutImagePath AS workoutImageURL
              FROM user_workout_progress uwp
              JOIN workouts w ON uwp.workout_id = w.workoutID
              WHERE uwp.user_routine_id = ?";

        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("i", $routine_progress_id);
        $stmt->execute();
        $result = $stmt->get_result();

        $workouts = [];
        while ($row = $result->fetch_assoc()) {
            $workouts[] = [
                "progress_id" => intval($row["progress_id"]),
                "workoutID" => intval($row["workoutID"]),
                "status" => $row["status"],
                "start_time" => $row["start_time"],
                "completion_time" => $row["completion_time"],
                "workoutName" => $row["workoutName"],
                "workoutDescription" => $row["workoutDescription"],
                "workoutImageURL" => $row["workoutImageURL"]
            ];
        }
        // Always return the array instead of only sending a response
        return $workouts;
    }
    // Get routine progress ID
    private function getRoutineProgressID($user_id, $routine_id)
    {
        $stmt = $this->conn->prepare("SELECT routine_progress_id FROM user_routine_progress WHERE user_id = ? AND routine_id = ?");
        $stmt->bind_param("ii", $user_id, $routine_id);
        $stmt->execute();

        // Explicitly initialize the variable
        $routine_progress_id = null;
        $stmt->bind_result($routine_progress_id);

        // Fetch and check if a value exists
        if ($stmt->fetch()) {
            $stmt->close();
            return $routine_progress_id;
        }

        $stmt->close();
        return null;  // Ensure it returns null if no record is found
    }
    // Insert routine progress
    private function insertRoutineProgress($user_id, $routine_id)
    {
        $stmt = $this->conn->prepare("INSERT INTO user_routine_progress (user_id, routine_id) VALUES (?, ?)");
        $stmt->bind_param("ii", $user_id, $routine_id);
        $stmt->execute();
        return $stmt->insert_id;
    }

    // Insert routine status
    private function insertRoutineStatus($routine_progress_id)
    {
        $stmt = $this->conn->prepare("INSERT INTO routine_status (routine_progress_id, status) VALUES (?, 'in-progress')");
        $stmt->bind_param("i", $routine_progress_id);
        $stmt->execute();
    }

    // Helper to send JSON response
    private function sendResponse($status, $data)
    {
        http_response_code($status);
        header('Content-Type: application/json');
        echo json_encode($data);
        exit();
    }

    // Fetch JSON request data
    private function getRequestData($requiredFields)
    {
        // Handle GET requests by using $_GET instead of reading JSON
        if ($_SERVER["REQUEST_METHOD"] === "GET") {
            $data = $_GET;
        } else {
            $data = isset($_SERVER['CONTENT_TYPE']) && $_SERVER['CONTENT_TYPE'] === 'application/json'
                ? json_decode(file_get_contents("php://input"), true)
                : $_POST;
        }

        foreach ($requiredFields as $field) {
            if (!isset($data[$field])) {
                $this->sendResponse(400, ["message" => "Missing required field: $field"]);
            }
        }

        return $data;
    }
}

$controller = new WorkoutProgressController($conn);
$controller->handleRequest();
?>