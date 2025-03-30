<?php
include(__DIR__ . '/../db_config.php');

error_reporting(E_ALL);
ini_set('display_errors', 1);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER["REQUEST_METHOD"] === "OPTIONS") {
    http_response_code(200);
    exit();
}

class RoutineController
{
    private $conn;

    public function __construct($dbConnection)
    {
        $this->conn = $dbConnection;
    }

    public function handleRequest()
    {
        $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
        // FIX: `routineID` instead of `routine_id`
        $routineId = isset($_GET['routineID']) ? intval($_GET['routineID']) : null;

        switch ($_SERVER["REQUEST_METHOD"]) {
            case "POST":
                $this->handlePostRequest();
                break;
            case "GET":
                if ($routineId !== null) {
                    // FIX: Correctly calling `fetchRoutineDetails`
                    $this->fetchRoutineDetails($routineId, $user_id);
                } else {
                    $this->fetchAllRoutines($user_id);
                }
                break;
            case "PUT":
                $this->respondWithJSON(["message" => "PUT method not supported"], 405);
                break;
            default:
                $this->respondWithJSON(["message" => "Invalid request method"], 405);
        }
    }


    /**
     * Fetch all routines with completion status
     */
    private function fetchAllRoutines($user_id = null)
    {
        $query = "SELECT 
            r.routineID, 
            r.routineName, 
            r.routineDescription, 
            r.routineDuration, 
            r.routineDifficulty,
            COUNT(rw.workoutID) AS exerciseCount";

        if ($user_id !== null) {
            // Correcting the query to use routine_progress_id to join with routine_status
            $query .= ", COALESCE((
                    SELECT MAX(CASE WHEN rs.status = 'completed' THEN 1 ELSE 0 END)
                    FROM routine_status rs
                    JOIN user_routine_progress urp ON rs.routine_progress_id = urp.routine_progress_id
                    WHERE urp.routine_id = r.routineID 
                    AND urp.user_id = ?
                ), 0) AS isCompleted";
        } else {
            $query .= ", 0 AS isCompleted";
        }

        $query .= " FROM routines r
            LEFT JOIN routine_workouts rw ON r.routineID = rw.routineID
            GROUP BY r.routineID";

        $stmt = $this->conn->prepare($query);

        if (!$stmt) {
            $this->respondWithJSON(["message" => "Failed to prepare statement", "error" => $this->conn->error], 500);
        }

        if ($user_id !== null) {
            $stmt->bind_param("i", $user_id);
        }

        $stmt->execute();
        $result = $stmt->get_result();

        $routines = [];
        while ($row = $result->fetch_assoc()) {
            $routines[] = $row;
        }

        if (empty($routines)) {
            $this->respondWithJSON([
                'success' => false,
                'message' => 'No routines found'
            ], 404);
        }

        // Return response as an object
        $response = [
            'success' => true,
            'data' => [
                'routines' => $routines
            ]
        ];
        $this->respondWithJSON($response);
    }


    /**
     * Fetch details of a single routine along with its workouts
     */
    private function fetchRoutineDetails($routineID, $user_id = null)
    {
        if (!is_int($routineID) || $routineID <= 0) {
            $this->respondWithJSON(["message" => "Invalid routine ID"], 400);
        }

        // Debugging logs to check if the parameters are received correctly
        error_log("DEBUG: Fetching routine with routineID = " . $routineID . " for user_id = " . $user_id);

        // Main query to get routine details by routineID
        $query = "SELECT 
            r.routineID, 
            r.routineName, 
            r.routineDescription, 
            r.routineDuration, 
            r.routineDifficulty";

        if ($user_id !== null) {
            // Correcting the query to use routine_progress_id to join with routine_status
            $query .= ", COALESCE((
                SELECT CASE 
                    WHEN rs.status = 'completed' THEN 1 
                    ELSE 0 
                END 
                FROM routine_status rs 
                JOIN user_routine_progress urp ON rs.routine_progress_id = urp.routine_progress_id
                WHERE urp.routine_id = r.routineID 
                AND urp.user_id = ?
            ), 0) AS isCompleted";
        } else {
            $query .= ", 0 AS isCompleted";
        }

        $query .= " FROM routines r WHERE r.routineID = ? LIMIT 1"; // Ensure fetching a single routine

        $stmt = $this->conn->prepare($query);
        if (!$stmt) {
            $this->respondWithJSON(["message" => "Database error", "error" => $this->conn->error], 500);
        }

        if ($user_id !== null) {
            $stmt->bind_param("ii", $user_id, $routineID);
        } else {
            $stmt->bind_param("i", $routineID);
        }

        $stmt->execute();
        $result = $stmt->get_result();
        $routine = $result->fetch_assoc();

        // If no routine is found, respond with an error
        if (!$routine) {
            $this->respondWithJSON(["message" => "Routine not found"], 404);
            exit();
        }

        // Fetch the workouts associated with this routine
        $routine["workouts"] = $this->fetchRoutineWorkouts($routineID, $user_id) ?? [];

        $this->respondWithJSON(['success' => true, 'data' => $routine]);
    }

    /**
     * Fetch workouts for a given routine
     */
    private function fetchRoutineWorkouts($routineID, $user_id = null)
    {
        $query = "SELECT 
                    w.workoutID, 
                    w.workoutName, 
                    w.workoutDescription,
                    CONCAT('http://10.0.2.2/workout_api/', w.workoutImagePath) AS workoutImageURL,
                    rw.sets, rw.reps, rw.duration,
                    COALESCE((
                        SELECT CASE 
                            WHEN uwp.status = 'completed' THEN 1 
                            ELSE 0 
                        END 
                        FROM user_workout_progress uwp 
                        JOIN user_routine_workouts urw ON uwp.user_routine_id = urw.user_routine_id
                        WHERE urw.routine_id = ? AND urw.workout_id = rw.workoutID
                    ), 0) AS isCompleted
                  FROM routine_workouts rw
                  JOIN workouts w ON rw.workoutID = w.workoutID
                  WHERE rw.routineID = ?";

        $stmt = $this->conn->prepare($query);
        $stmt->bind_param("ii", $routineID, $routineID);
        $stmt->execute();
        $result = $stmt->get_result();

        $workouts = [];
        while ($row = $result->fetch_assoc()) {
            $row["isCompleted"] = ($row["isCompleted"] == 1) ? true : false;
            $workouts[] = $row;
        }

        return $workouts;
    }

    /**
     * Handle POST request for creating a new routine
     */
    private function handlePostRequest()
    {
        $data = json_decode(file_get_contents("php://input"), true);

        // Log received data for debugging
        error_log(print_r($data, true));

        // Check if required fields are set
        if (!isset($data['routineName'], $data['routineDescription'], $data['routineDuration'], $data['routineDifficulty'], $data['workouts'])) {
            $this->respondWithJSON(["message" => "Missing required fields", "received_data" => $data], 400);
        }

        // Insert the routine into the database
        $stmt = $this->conn->prepare("INSERT INTO routines (routineName, routineDescription, routineDuration, routineDifficulty) VALUES (?, ?, ?, ?)");
        $stmt->bind_param("ssis", $data['routineName'], $data['routineDescription'], $data['routineDuration'], $data['routineDifficulty']);
        if (!$stmt->execute()) {
            $this->respondWithJSON(["message" => "Failed to insert routine", "error" => $this->conn->error], 500);
        }

        // Get the last inserted routineID
        $routineID = $this->conn->insert_id;

        // Insert each workout into the routine_workouts table
        $workoutInsertSuccess = true;
        $stmt = $this->conn->prepare("INSERT INTO routine_workouts (routineID, workoutID, sets, reps, duration) VALUES (?, ?, ?, ?, ?)");

        foreach ($data['workouts'] as $workout) {
            $stmt->bind_param("iiiii", $routineID, $workout['workoutID'], $workout['sets'], $workout['reps'], $workout['duration']);
            if (!$stmt->execute()) {
                $workoutInsertSuccess = false;
                error_log("Failed to insert workoutID " . $workout['workoutID'] . ": " . $this->conn->error);
            }
        }

        if (!$workoutInsertSuccess) {
            $this->respondWithJSON(["message" => "Routine created, but some workouts failed to insert"], 500);
        }

        // Return success response
        $this->respondWithJSON([
            "message" => "Routine and workouts added successfully",
            "routineID" => $routineID
        ], 201);
    }

    /**
     * Respond with JSON
     */
    private function respondWithJSON($data, $statusCode = 200)
    {
        http_response_code($statusCode);
        echo json_encode($data);
        exit();
    }
}

$controller = new RoutineController($conn);
$controller->handleRequest();
