package com.example.tictactoe1

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

enum class GameStatus {
    CREATED, WAITING_FOR_PLAYER, IN_PROGRESS, FINISHED
}

data class GameModel(
    var gameId: String = "",
    var filledPos: MutableList<String> = MutableList(9) { "" },
    var winner: String = "",
    var gameStatus: GameStatus = GameStatus.CREATED,
    var currentPlayer: String = "",
    var player1: String? = null,
    var player2: String? = null
)

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var gameModel by mutableStateOf(GameModel())
    private var isGameStarted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            if (auth.currentUser == null) {
                AuthenticationScreen()
            } else {
                if (isGameStarted) {
                    GameScreen(gameModel, ::handleTileClick, ::playAgain, ::leaveGame)
                } else {
                    MainScreen()
                }
            }
        }
    }

    @Composable
    fun AuthenticationScreen() {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isRegistering by remember { mutableStateOf(false) }
        var passwordVisibility by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Email Input
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                        Icon(image, contentDescription = "Toggle password visibility")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Register or Login Button
            Button(
                onClick = {
                    if (isRegistering) registerUser(email, password) else signInWithEmail(email, password)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegistering) "Register" else "Login")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle Registration/Login Text
            TextButton(onClick = { isRegistering = !isRegistering }) {
                Text(if (isRegistering) "Already have an account? Log in" else "Need an account? Register")
            }
        }
    }

// Firebase Authentication Register user
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_LONG).show()
                setContent { MainScreen() } // Navigate to MainScreen on success
            } else {
                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
// Firebase Authentication Sign In sign in
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_LONG).show()
                setContent { MainScreen() } // Navigate to MainScreen on success
            } else {
                Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { createOnlineGame(context) }) {
                Text("Create Online Game")
            }
            Spacer(modifier = Modifier.height(16.dp))
            var gameId by remember { mutableStateOf("") }
            TextField(
                value = gameId,
                onValueChange = { gameId = it },
                label = { Text("Enter Game ID") }
            )
            Button(onClick = { joinOnlineGame(gameId, context) }) {
                Text("Join Online Game")
            }
        }
    }

    @Composable
    fun GameScreen(
        gameModel: GameModel,
        onTileClick: (Int) -> Unit,
        onPlayAgain: () -> Unit,
        onLeaveGame: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button at the top
            Button(
                onClick = onLeaveGame,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Game ID: ${gameModel.gameId}")
            Text(
                when (gameModel.gameStatus) {
                    GameStatus.IN_PROGRESS -> if (gameModel.currentPlayer == auth.currentUser?.uid) "Your Turn" else "Opponent's Turn"
                    GameStatus.FINISHED -> if (gameModel.winner.isNotEmpty()) "Winner: ${gameModel.winner}" else "Draw"
                    else -> "Waiting for Player"
                }
            )
            if (gameModel.gameStatus == GameStatus.FINISHED) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onPlayAgain) {
                    Text("Play Again")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onLeaveGame) {
                    Text("Leave Game")
                }
            } else {
                GameBoard(gameModel.filledPos, onTileClick)
            }
        }
    }


    @Composable
    fun GameBoard(board: List<String>, onTileClick: (Int) -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (i in 0..2) {
                Row {
                    for (j in 0..2) {
                        val index = i * 3 + j
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .padding(4.dp)
                                .clickable(
                                    enabled = board[index].isEmpty() && gameModel.gameStatus == GameStatus.IN_PROGRESS
                                ) { onTileClick(index) }
                                .background(
                                    when (board[index]) {
                                        "X" -> Color.Red
                                        "O" -> Color.Blue
                                        else -> Color.Gray
                                    }
                                )
                                .border(2.dp, Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = board[index],
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 40.sp), // Increased font size
                                color = Color.White // Optional: Adjust text color for better contrast
                            )
                        }

                    }
                }
            }
        }
    }

    private fun handleTileClick(index: Int) {
        if (gameModel.filledPos[index].isEmpty() && gameModel.currentPlayer == auth.currentUser?.uid) {
            // Immediate local update
            gameModel = gameModel.copy(
                filledPos = gameModel.filledPos.toMutableList().apply { this[index] =
                    if (gameModel.currentPlayer == gameModel.player1) "X" else "O" },
                currentPlayer = if (gameModel.currentPlayer == gameModel.player1) gameModel.player2!! else gameModel.player1!!
            )

            // Check for winner locally
            checkWinner()

            // Push updated state to Firebase
            firestore.collection("games").document(gameModel.gameId).set(gameModel)
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update game: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkWinner() {
        val winningPositions = arrayOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (positions in winningPositions) {
            if (positions.all { gameModel.filledPos[it] == "X" }) {
                gameModel.winner = "X"
                gameModel.gameStatus = GameStatus.FINISHED
                return
            } else if (positions.all { gameModel.filledPos[it] == "O" }) {
                gameModel.winner = "O"
                gameModel.gameStatus = GameStatus.FINISHED
                return
            }
        }
        if (gameModel.filledPos.all { it.isNotEmpty() }) {
            gameModel.gameStatus = GameStatus.FINISHED
        }
    }

    private fun createOnlineGame(context: android.content.Context) {
        val gameId = Random.nextInt(1000, 9999).toString()
        val newGame = GameModel(
            gameId = gameId,
            player1 = auth.currentUser?.uid,
            currentPlayer = auth.currentUser?.uid ?: "",
            gameStatus = GameStatus.WAITING_FOR_PLAYER
        )
        firestore.collection("games").document(gameId).set(newGame)
            .addOnSuccessListener {
                gameModel = newGame
                isGameStarted = true
                listenToGameUpdates(gameId)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to create game: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun joinOnlineGame(gameId: String, context: android.content.Context) {
        firestore.collection("games").document(gameId).get()
            .addOnSuccessListener { document ->
                val game = document.toObject(GameModel::class.java)
                if (game != null) {
                    // Check if the game is in progress or waiting for players
                    if (game.player1 == auth.currentUser?.uid || game.player2 == auth.currentUser?.uid) {
                        // Player is already part of the game; just load the game state
                        gameModel = game
                        isGameStarted = true
                        listenToGameUpdates(gameId)
                        setContent { GameScreen(gameModel, ::handleTileClick, ::playAgain, ::leaveGame) }
                    } else if (game.player2.isNullOrEmpty()) {
                        // Join as Player 2
                        game.player2 = auth.currentUser?.uid
                        game.gameStatus = GameStatus.IN_PROGRESS
                        firestore.collection("games").document(gameId).set(game).addOnSuccessListener {
                            gameModel = game
                            isGameStarted = true
                            listenToGameUpdates(gameId)
                            setContent { GameScreen(gameModel, ::handleTileClick, ::playAgain, ::leaveGame) }
                        }
                    } else {
                        Toast.makeText(context, "Game is already full.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Game not found.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to join game: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun listenToGameUpdates(gameId: String) {
        firestore.collection("games").document(gameId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val updatedGame = snapshot.toObject(GameModel::class.java)
                if (updatedGame != null) {
                    // Only update if the snapshot reflects a newer game state
                    if (updatedGame != gameModel) {
                        gameModel = updatedGame
                    }
                }
            }
        }
    }

    private fun playAgain() {
        gameModel = gameModel.copy(
            filledPos = MutableList(9) { "" },
            winner = "",
            gameStatus = GameStatus.IN_PROGRESS,
            currentPlayer = gameModel.player1 ?: ""
        )
        firestore.collection("games").document(gameModel.gameId).set(gameModel)
    }

    private fun leaveGame() {
        if (auth.currentUser?.uid == gameModel.player1) {
            if (!gameModel.player2.isNullOrEmpty()) {
                // If Player 1 leaves, assign Player 2 as Player 1 and reset the game
                gameModel = gameModel.copy(
                    player1 = gameModel.player2,
                    player2 = null,
                    currentPlayer = gameModel.player2 ?: "",
                    gameStatus = GameStatus.WAITING_FOR_PLAYER,
                    filledPos = MutableList(9) { "" }, // Clear the board
                    winner = "" // Reset winner
                )
            } else {
                // If Player 1 leaves and no Player 2, reset the game
                gameModel = gameModel.copy(
                    player1 = null,
                    currentPlayer = "",
                    gameStatus = GameStatus.CREATED,
                    filledPos = MutableList(9) { "" }, // Clear the board
                    winner = "" // Reset winner
                )
            }
        } else if (auth.currentUser?.uid == gameModel.player2) {
            // If Player 2 leaves, reset the game for Player 1 to wait for a new player
            gameModel = gameModel.copy(
                player2 = null,
                currentPlayer = gameModel.player1 ?: "",
                gameStatus = GameStatus.WAITING_FOR_PLAYER,
                filledPos = MutableList(9) { "" }, // Clear the board
                winner = "" // Reset winner
            )
        }

        // Update the game in Firestore
        firestore.collection("games").document(gameModel.gameId).set(gameModel).addOnCompleteListener {
            isGameStarted = false
            // Return to the MainScreen for the player who left
            setContent { MainScreen() }
        }
    }


}
