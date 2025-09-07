# Smart Task Scheduler with Priority Queues

## Overview

**Smart Task Scheduler** is a Java desktop application built with Swing for effective personal task management. It enables users to add, edit, delete, filter, 
and prioritize tasks by urgency and deadline using a priority queue, and provides pop-up reminders for approaching deadlines. Tasks are saved to disk and can be reloaded at any time.

***

## Features

- Create, edit, and delete tasks with a title, priority (1-highest to 5-lowest), and deadline
- Tasks are displayed in order of priority and deadline using a priority queue
- Pop-up reminders appear for tasks due soon (within 5 minutes)
- Save and load all tasks to a file (using Java serialization for persistence)
- Filter to show only today's or high-priority tasks
- User-friendly Swing-based GUI

***

## Installation and Running

### Prerequisites

- Java JDK 8 or higher installed
- Command-line access (`cmd`, Terminal, etc.)

### Steps

1. **Clone or download the source code.**

2. **Save the main code as `SmartTaskScheduler.java` in a folder.**

3. **Compile the source:**
   ```cmd
   javac SmartTaskScheduler.java
   ```

4. **Create a manifest file called `manifest.txt` with:**
   ```
   Main-Class: SmartTaskScheduler
   ```
   *(Ensure there is a blank line at the end)*

5. **Package into a runnable `.jar`:**
   ```cmd
   jar cfm SmartTaskScheduler.jar manifest.txt *.class
   ```

6. **Run the application:**
   ```cmd
   java -jar SmartTaskScheduler.jar
   ```

***

## Usage

- **Add Task:** Click "Add Task", fill all details, and confirm.
- **Edit Task:** Select a task in the table and click "Edit Task".
- **Delete Task:** Select and click "Delete Task".
- **Save Tasks:** Click "Save Tasks" to store the current list in a file.
- **Load Tasks:** Click "Load Tasks" to restore tasks from a file.
- **Reminders:** A popup notification appears for tasks with deadlines within the next 5 minutes.
- **Filtering:** Use filter buttons to view today’s tasks or only high-priority items, or click "Show All" to return to the full list.

***
## Contributing

Pull requests and suggestions for new features and improvements are welcome!

***

## License

This project is provided for educational purposes.
