package com.uc3m.travelapp.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uc3m.travelapp.FirebaseManager
import com.uc3m.travelapp.R
import com.uc3m.travelapp.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(tripId: String, onBackClick: () -> Unit) {

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Trip info needed to send notifications to the author (MA-07)
    var tripAuthorId by remember { mutableStateOf("") }
    var tripDestination by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Load trip info to get the author for chat notifications (MA-07)
    LaunchedEffect(tripId) {
        FirebaseManager.firestore
            .collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { snapshot ->
                tripAuthorId = snapshot.getString("authorId") ?: ""
                tripDestination = snapshot.getString("destination") ?: ""
            }
    }

    // Load messages in real time using addSnapshotListener (MA-04)
    // Messages stored in subcollection: chats/{tripId}/messages
    LaunchedEffect(tripId) {
        FirebaseManager.firestore
            .collection("chats")
            .document(tripId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    errorMessage = error.message
                        ?: context.getString(R.string.chat_error_loading)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { document ->
                        document.toObject(Message::class.java)
                    }
                    // Scroll to bottom automatically when new messages arrive (MA-02)
                    if (messages.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chat_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.chat_cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->

        // imePadding pushes the input field above the keyboard when it appears (MA-02)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage.isNotEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    messages.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.chat_no_messages_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.chat_no_messages_subtitle),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                            items(messages) { message ->
                                MessageBubble(message = message)
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }

            // Input row - stays above keyboard thanks to imePadding (MA-02)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text(stringResource(R.string.chat_message_placeholder)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            sendMessage(
                                tripId = tripId,
                                messageText = messageText,
                                tripAuthorId = tripAuthorId,
                                tripDestination = tripDestination,
                                context = context,
                                onSending = { isSending = it },
                                onMessageSent = { messageText = "" },
                                onError = { errorMessage = it }
                            )
                        }
                    )
                )

                IconButton(
                    onClick = {
                        sendMessage(
                            tripId = tripId,
                            messageText = messageText,
                            tripAuthorId = tripAuthorId,
                            tripDestination = tripDestination,
                            context = context,
                            onSending = { isSending = it },
                            onMessageSent = { messageText = "" },
                            onError = { errorMessage = it }
                        )
                    },
                    enabled = !isSending
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send_cd),
                        tint = if (isSending) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Reusable composable for each message bubble in the chat (MA-02)
// Own messages appear on the right in primary color, others on the left in surfaceVariant
@Composable
fun MessageBubble(message: Message) {
    val currentUserId = FirebaseManager.currentUser?.uid
    val isOwnMessage = message.authorId == currentUserId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isOwnMessage) {
                Text(
                    text = message.authorEmail,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOwnMessage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// Helper function to send a message and optionally notify the trip author (MA-04, MA-07)
// Extracted to avoid duplicating logic between button click and keyboard action
private fun sendMessage(
    tripId: String,
    messageText: String,
    tripAuthorId: String,
    tripDestination: String,
    context: android.content.Context,
    onSending: (Boolean) -> Unit,
    onMessageSent: () -> Unit,
    onError: (String) -> Unit
) {
    if (messageText.isBlank()) return

    val currentUser = FirebaseManager.currentUser ?: return
    onSending(true)

    val message = Message(
        text = messageText.trim(),
        authorId = currentUser.uid,
        authorEmail = currentUser.email ?: ""
    )

    FirebaseManager.firestore
        .collection("chats")
        .document(tripId)
        .collection("messages")
        .add(message)
        .addOnSuccessListener {
            onSending(false)
            onMessageSent()

            // Save in-app notification for the trip author if someone else writes (MA-07)
            // We skip notification if the author is writing in their own chat
            if (tripAuthorId.isNotEmpty() && tripAuthorId != currentUser.uid) {
                val notificationData = mapOf(
                    "fromEmail" to (currentUser.email ?: ""),
                    "tripDestination" to tripDestination,
                    "tripId" to tripId,
                    "type" to "chat",
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                FirebaseManager.firestore
                    .collection("notifications")
                    .document(tripAuthorId)
                    .collection("items")
                    .add(notificationData)
            }
        }
        .addOnFailureListener { exception ->
            onSending(false)
            onError(exception.message ?: context.getString(R.string.chat_error_sending))
        }
}