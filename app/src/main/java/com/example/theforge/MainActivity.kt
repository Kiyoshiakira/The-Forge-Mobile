package com.example.theforge

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.theforge.ui.theme.TheForgeTheme
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Data Classes ---
data class User(val name: String, var bio: String, var avatarUri: Uri? = null)
data class Tab(val title: String, var content: String = "")
enum class ToolType(val displayName: String, val icon: ImageVector) {
    Story("Story", Icons.Outlined.Book),
    Script("Script", Icons.Default.Movie),
    Poetry("Poetry", Icons.Outlined.Edit),
    Comic("Comic", Icons.Default.Book), // Placeholder
    Manga("Manga", Icons.Default.Book), // Placeholder
    Custom("Custom", Icons.Default.Construction)
}
data class Tool(val title: String, val type: ToolType, var imageUri: Uri? = null, val tabs: MutableList<Tab>)
data class Project(val name: String, val owner: User, var bannerUri: Uri? = null, val tools: MutableList<Tool> = mutableStateListOf(), val collaborators: MutableList<User> = mutableStateListOf())

// --- Screen Navigation ---
sealed class Screen {
    object Splash : Screen()
    object ProjectList : Screen()
    data class ProjectLanding(val project: Project) : Screen()
    data class ProjectEditor(val project: Project) : Screen()
    data class Profile(val user: User) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TheForgeTheme {
                TheForgeApp()
            }
        }
    }
}

@Composable
fun TheForgeApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val currentUser = remember { User("Alex", "A creative mind exploring new worlds.") }
    val projects = remember {
        mutableStateListOf(
            Project("Skylantia Series: Planet Viridia", owner = currentUser, tools = mutableStateListOf(Tool("Main Story", ToolType.Story, tabs = mutableStateListOf(Tab("Chapter 1")))), collaborators = mutableStateListOf(currentUser))
        )
    }

    when (val screen = currentScreen) {
        is Screen.Splash -> SplashScreen { currentScreen = Screen.ProjectList }
        is Screen.ProjectList -> ProjectListScreen(
            projects = projects,
            onProjectClick = { project -> currentScreen = Screen.ProjectLanding(project) },
            onAddProject = { projectName -> projects.add(Project(name = projectName, owner = currentUser, collaborators = mutableStateListOf(currentUser), tools = mutableStateListOf(Tool("New Tool", ToolType.Story, tabs = mutableStateListOf(Tab("First Tab")))))) },
            onProfileClick = { currentScreen = Screen.Profile(currentUser) }
        )
        is Screen.ProjectLanding -> ProjectLandingScreen(
            project = screen.project,
            onOpenEditor = { currentScreen = Screen.ProjectEditor(screen.project) },
            onNavigateBack = { currentScreen = Screen.ProjectList }
        )
        is Screen.ProjectEditor -> ProjectEditorScreen(
            project = screen.project,
            onProfileClick = { currentScreen = Screen.Profile(currentUser) },
            onNavigateBack = { currentScreen = Screen.ProjectLanding(screen.project) }
        )
        is Screen.Profile -> ProfileScreen(
            user = screen.user, 
            projects = projects.filter { it.collaborators.contains(screen.user) },
            onNavigateBack = { currentScreen = Screen.ProjectList }
        )
    }
}

// --- Screens ---

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Book, contentDescription = "The Forge Logo", modifier = Modifier.size(128.dp))
        Text("The Forge", style = MaterialTheme.typography.headlineLarge)
        Text("Project Building Suite for Creators. AI-Powered. Collaborative.")
    }
    LaunchedEffect(Unit) { delay(2000); onTimeout() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(projects: List<Project>, onProjectClick: (Project) -> Unit, onAddProject: (String) -> Unit, onProfileClick: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) { AddProjectDialog(onDismiss = { showDialog = false }, onAddProject = { onAddProject(it); showDialog = false }) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Projects") }, actions = { IconButton(onClick = onProfileClick) { Icon(Icons.Default.Person, "Profile") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Create New Project") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(projects) { project ->
                ProjectCard(project, onClick = { onProjectClick(project) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectLandingScreen(project: Project, onOpenEditor: () -> Unit, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Project Details") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item { ProjectBanner(project) }
            item { ProjectHeader(project) }
            item {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = onOpenEditor) { Text("Open Editor") }
                    Button(onClick = { /* TODO */ }) { Text("Edit Project") }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Forum / Comments", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("Add a comment...") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorScreen(project: Project, onProfileClick: () -> Unit, onNavigateBack: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedToolIndex by rememberSaveable { mutableIntStateOf(if (project.tools.isNotEmpty()) 0 else -1) }
    var showAddToolDialog by remember { mutableStateOf(false) }
    var showFormattingSheet by remember { mutableStateOf(false) }

    if (showAddToolDialog) { AddToolDialog(onDismiss = { showAddToolDialog = false }, onAddTool = { name, type -> project.tools.add(Tool(name, type, tabs = mutableStateListOf(Tab("New Tab")))); selectedToolIndex = project.tools.lastIndex }) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { ModalDrawerSheet { CollaborationHubContent(project) } }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(project.name) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    actions = {
                        IconButton(onClick = onProfileClick) { Icon(Icons.Default.Person, "Profile") }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Group, "Collaboration Hub") }
                    }
                )
            }
        ) { padding ->
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                NavigationRail {
                    project.tools.forEachIndexed { index, tool ->
                        NavigationRailItem(
                            icon = { Icon(tool.type.icon, null) },
                            label = { Text(tool.title) },
                            selected = selectedToolIndex == index,
                            onClick = { selectedToolIndex = index }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                     IconButton(onClick = { showAddToolDialog = true }) { Icon(Icons.Default.Add, "Add Tool") }
                }
                if (selectedToolIndex != -1 && selectedToolIndex < project.tools.size) {
                    key(project.tools[selectedToolIndex]) {
                        ToolContent(tool = project.tools[selectedToolIndex], onShowFormatting = { showFormattingSheet = true })
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Book, contentDescription = null, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No tools selected.", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    if (showFormattingSheet) {
        ModalBottomSheet(onDismissRequest = { showFormattingSheet = false }) {
            FormattingBottomSheetContent(state = rememberRichTextState()) // This state will need to be hoisted
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: User, projects: List<Project>, onNavigateBack: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editingBio by remember { mutableStateOf(user.bio) }
    var avatarUri by rememberSaveable { mutableStateOf(user.avatarUri) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> 
        if(uri != null) {
            avatarUri = uri
            user.avatarUri = uri 
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(user.name) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (isEditing) {
                        // Save button when editing
                        IconButton(onClick = {
                            user.bio = editingBio
                            isEditing = false
                        }) { Icon(Icons.Default.Done, "Save Changes") }
                    } else {
                        // Edit button when not editing
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Default.Edit, "Edit Profile") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (isEditing) {
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                placeholder = rememberVectorPainter(Icons.Default.Person),
                error = rememberVectorPainter(Icons.Default.Person),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editingBio,
                    onValueChange = { editingBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Projects", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(projects) { project ->
                    Text(project.name, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// --- Components ---

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = project.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun ProjectBanner(project: Project) {
    var bannerUri by rememberSaveable { mutableStateOf(project.bannerUri) }
    val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            bannerUri = uri
            project.bannerUri = uri
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { pickBanner.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        contentAlignment = Alignment.Center
    ) {
        if (bannerUri != null) {
            AsyncImage(model = bannerUri, contentDescription = "Project Banner", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Banner", modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun ProjectHeader(project: Project) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(project.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Created by ${project.owner.name}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Collaborators", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.collaborators) { user ->
                AsyncImage(model = user.avatarUri, placeholder = rememberVectorPainter(Icons.Default.Person), contentDescription = user.name, modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
    }
}

@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onAddProject: (String) -> Unit) {
    var newProjectName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Project") },
        text = { OutlinedTextField(value = newProjectName, onValueChange = { newProjectName = it }, label = { Text("Project Name") }, singleLine = true) },
        confirmButton = { Button(onClick = { onAddProject(newProjectName) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddToolDialog(onDismiss: () -> Unit, onAddTool: (String, ToolType) -> Unit) {
    var toolName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ToolType.Story) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Tool") },
        text = {
            Column {
                OutlinedTextField(value = toolName, onValueChange = { toolName = it }, label = { Text("Tool Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("Choose a tool type:")
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(ToolType.values()) { toolType ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedType = toolType }.padding(vertical = 4.dp)) {
                            RadioButton(selected = selectedType == toolType, onClick = { selectedType = toolType })
                            Text(toolType.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if(toolName.isNotBlank()) onAddTool(toolName, selectedType) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CollaborationHubContent(project: Project) {
    var showInviteDialog by remember { mutableStateOf(false) }

    if (showInviteDialog) {
        var inviteeName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Collaborator") },
            text = { OutlinedTextField(value = inviteeName, onValueChange = { inviteeName = it }, label = { Text("Username or Email")}) },
            confirmButton = {
                Button(onClick = {
                    if (inviteeName.isNotBlank()) {
                        project.collaborators.add(User(inviteeName, "New collaborator"))
                        showInviteDialog = false
                    }
                }) { Text("Invite") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Collaboration Hub", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Collaborators", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showInviteDialog = true }) { Text("Invite") }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(project.collaborators) { user ->
                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = user.avatarUri, placeholder = rememberVectorPainter(Icons.Default.Person), contentDescription = "User Avatar", modifier = Modifier.size(40.dp).clip(CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(user.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Project Chat", style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.weight(1f)) { /* Chat messages would go here */ }
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Send a message") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically){
            Text("AI assistance is enabled on this project.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolContent(tool: Tool, modifier: Modifier = Modifier, onShowFormatting: () -> Unit) {
    val richTextState = rememberRichTextState()
    val currentTab = tool.tabs.firstOrNull()

    LaunchedEffect(currentTab) {
        if (currentTab != null) {
            richTextState.setHtml(currentTab.content)
        } else {
            richTextState.setHtml("")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (currentTab != null) {
                currentTab.content = richTextState.toHtml()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        EditorToolbar(richTextState, onShowFormatting)
        RichTextEditor(
            state = richTextState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun EditorToolbar(state: RichTextState, onShowFormatting: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = { state.toggleBold() }) {
            Icon(
                imageVector = Icons.Default.FormatBold, 
                contentDescription = "Bold",
                tint = if (state.isBold) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        IconButton(onClick = { state.toggleItalic() }) { 
            Icon(
                imageVector = Icons.Default.FormatItalic, 
                contentDescription = "Italic",
                tint = if (state.isItalic) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        IconButton(onClick = { state.toggleUnderline() }) { 
            Icon(
                imageVector = Icons.Default.FormatUnderlined, 
                contentDescription = "Underline",
                tint = if (state.isUnderline) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onShowFormatting) { Icon(Icons.Default.Tune, "More Formatting") }
    }
}

@Composable
fun FormattingBottomSheetContent(state: RichTextState) {
    // Placeholder for more advanced formatting options
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Advanced Formatting", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Alignment, Font Size, Color, etc. will go here.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProjectEditorScreenPreview() {
    TheForgeTheme {
        val owner = User("Alex", "");
        val project = Project("Skylantia Series", owner, collaborators = mutableStateListOf(owner, User("Jane", "")))
        project.tools.add(Tool("Main Story", ToolType.Story, tabs = mutableStateListOf(Tab("Chapter 1"))))
        ProjectEditorScreen(project = project, onProfileClick = {}, onNavigateBack = {})
    }
}
