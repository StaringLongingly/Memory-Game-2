package com.raymarchers;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_RENDERER;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.GL_VENDOR;
import static org.lwjgl.opengl.GL11C.GL_VERSION;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glClearColor;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL11C.glGetString;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glDeleteBuffers;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20C.glAttachShader;
import static org.lwjgl.opengl.GL20C.glCompileShader;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL20C.glCreateShader;
import static org.lwjgl.opengl.GL20C.glDeleteProgram;
import static org.lwjgl.opengl.GL20C.glDeleteShader;
import static org.lwjgl.opengl.GL20C.glDetachShader;
import static org.lwjgl.opengl.GL20C.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20C.glGetShaderi;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glLinkProgram;
import static org.lwjgl.opengl.GL20C.glShaderSource;
import static org.lwjgl.opengl.GL20C.glUniform1f;
import static org.lwjgl.opengl.GL20C.glUniform1fv;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL20C.glValidateProgram;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// A Slot is a letter with some extra info
class Slot {
    public int symbol;
	public float t;
	public float cooldown;
    public boolean peek;
    public boolean revealed;

    public Slot(int symbol) {
        this.symbol = symbol;
        this.peek = false;
        this.revealed = false;
    }

    public Slot() {
        this.peek = false;
        this.revealed = false;
    }

    public float getSymbol(float delta) {
		float delta2 = delta * 100f;
		if (cooldown >= 0f) {
			cooldown -= delta2;
			t = 0.5f;
		} else {
			if (peek || revealed) {
				t += delta2;
				if (t >= 0.5f) t = 0.5f;
			} else {
				t -= delta2;
				if (t <= 0.0f) t = 0.0f;
			}
		}
        return symbol + t;
    }
}

// Handles initialization and gameplay
class GameManager {
    public Slot[][] slots;
	public String playerName;

	public int gameSizeX;
	public int gameSizeY;

	public int tries;
	public int currentTries;
	public int maxTries;
	
	public int lastPickedX;
	public int lastPickedY;
	public int firstPickedX;
	public int firstPickedY;
	public int secondPickedX;
	public int secondPickedY;

    public GameManager(int gameSizeX, int gameSizeY, int maxTries, int shuffleCount, boolean reveal) {
        System.out.println("Creating a New Game!");

		playerName = "";

		tries = 0;
		currentTries = 0;
		this.maxTries = maxTries;

		this.gameSizeX = gameSizeX;
		this.gameSizeY = gameSizeY;

		firstPickedX = firstPickedY = secondPickedX = secondPickedY = lastPickedX = lastPickedY = -1;

        slots = new Slot[gameSizeY][gameSizeX];

        int totalSlots = gameSizeX * gameSizeY;
        int pairCount = totalSlots / 2;

        List<Integer> charList = new ArrayList<Integer>();
        Random random = new Random();
        for (int i = 0; i < pairCount; i++) {
            int randomChar = (random.nextInt(100));
			if (charList.contains(randomChar)) {
				i--;
			}
			else {
            	charList.add(randomChar);
            	charList.add(randomChar);
			}
        }

        // Shuffle the generated characters
        Collections.shuffle(charList, random);

        // Fill the slots
        int index = 0;
        for (int i = 0; i < slots.length; i++) {
            for (int j = 0; j < slots[i].length; j++) {
                slots[i][j] = new Slot(charList.get(index));
                slots[i][j].revealed = reveal;
                slots[i][j].cooldown = 0.5f;
                index++;
            }
        }

        // Additional shuffling if needed
        for (int i = 0; i < shuffleCount; i++) {
            shuffle();
        }
    }

    // Shuffles two random slots
    public void shuffle() {
        Random random = new Random();
        int x1 = random.nextInt(slots.length);
        int x2 = random.nextInt(slots.length);

        int y1 = random.nextInt(slots[0].length);
        int y2 = random.nextInt(slots[0].length);

        int tmp = slots[x1][y1].symbol;
        slots[x1][y1].symbol = slots[x2][y2].symbol;
        slots[x2][y2].symbol = tmp;
    }

	// -1 for invalid, 0 for unmatched, 1 for matched 
	public int clicked(int clickedX, int clickedY) {
		int result = -1;
		boolean firstSelected = firstPickedX != -1 && firstPickedY != -1;
		boolean secondSelected = secondPickedX != -1 && secondPickedY != -1;
		
		// If the first pick was clicked again
		if (clickedX == firstPickedX && clickedY == firstPickedY && firstSelected) {
			// Unselect it
			System.out.println("First Shape Unselected");
			slots[firstPickedY][firstPickedX].peek = false;
			firstPickedX = firstPickedY = -1;
			return -1;
		}
		// If a new pick is selected
		else if (!firstSelected) {
			System.out.println("First Shape Picked");
			firstPickedX = clickedX;
			firstPickedY = clickedY;
			slots[firstPickedY][firstPickedX].peek = true;
			if (secondSelected)
				slots[secondPickedY][secondPickedX].peek = false;
			secondPickedX = secondPickedY = -1;
		}
		// If a new second pick is selected
		else if (!secondSelected) {
			System.out.println("Second Shape Selected");
			secondPickedX = clickedX;
			secondPickedY = clickedY;
			// Show the second pick and hide the first one
			slots[secondPickedY][secondPickedX].peek = true;
			slots[secondPickedY][secondPickedX].peek = false;
			lastPickedY = clickedY;
			lastPickedX = clickedX;
		}

		// Update bools for proper testing
		firstSelected = firstPickedX != -1 && firstPickedY != -1;
		secondSelected = secondPickedX != -1 && secondPickedY != -1;
		// If both are selected, test
		if (firstSelected && secondSelected) {
			result = compareSlots(firstPickedX, firstPickedY, secondPickedX, secondPickedY);
		
			// If it was a match
			if (result == 1) {
				slots[firstPickedY][firstPickedX].revealed = true;
				slots[secondPickedY][secondPickedX].revealed = true;

				slots[firstPickedY][firstPickedX].peek = false;
				slots[secondPickedY][secondPickedX].peek = false;
			}
			// If it wasnt
			else if (result == 0) {
				slots[firstPickedY][firstPickedX].peek = false;
				slots[secondPickedY][secondPickedX].peek = false;
				slots[firstPickedY][firstPickedX].cooldown = 0.5f;
				slots[secondPickedY][secondPickedX].cooldown = 0.5f;
			}
			
			// And reset
			firstPickedX = firstPickedY = -1;
		}

		return result;
	}

    public int compareSlots(int x1, int y1, int x2, int y2) {
		System.out.print("    ");
        // Check bounds
        if (x1 < 0 || x1 >= slots.length ||
            x2 < 0 || x2 >= slots.length ||
            y1 < 0 || y1 >= slots[0].length ||
            y2 < 0 || y2 >= slots[0].length) {
            System.out.println("Invalid coordinates! Please try again:");
            return -1;
        }

        // Check if same slot chosen twice (Only for Console)
        if (x1 == x2 && y1 == y2) {
            System.out.println("You cannot give the same coordinates for both letters. Try again..");
            return -1;
        }

        // Compare symbols
        if (slots[y1][x1].symbol == slots[y2][x2].symbol) {
            System.out.println("Successfully revealed a pair!");
            return 1;
        } else {
            System.out.println("Not the same letter. Try again..");
            return 0;
        }
    }

    // Checks if all slots are revealed
    public boolean allRevealed() {
        for (int i = 0; i < slots.length; i++) {
            for (int j = 0; j < slots[i].length; j++) {
                if (!slots[i][j].revealed) {
                    return false;
                }
            }
        }
        return true;
    }

    // Prints and formats all the slots
    public void printSlotsConsole(float delta) {
        System.out.print("    ");
        for (int j = 0; j < slots[0].length; j++) {
            System.out.print(j + "   ");
        }
        System.out.println();

        for (int i = 0; i < slots.length; i++) {
            System.out.print(i + "  ");
            for (int j = 0; j < slots[i].length; j++) {
				float slotChar = slots[i][j].getSymbol(delta);
                System.out.print(slotChar);
                if (j < slots[i].length - 1) System.out.print(" | ");
            }
            System.out.println();
        }
    }

	public float[] getCharsAsFloats(float delta) {
		float[] result = new float[gameSizeX * gameSizeY];
		int k = 0;
		for (int i = 0; i < gameSizeY; i++) {
			for (int j = 0; j < gameSizeX; j++) {
				result[k] = slots[i][j].getSymbol(delta);
				k++;
			}
		}
		return result;
	}
}

class Shader {
	private String vertexFile = "vertex.glsl";
	private String fragmentFile = "fragment.glsl";
	
	private int programID, vertexID, fragmentID;
	
	public Shader() {}
	
	public Shader(String vertexFile, String fragmentFile) {
		this.vertexFile = vertexFile;
		this.fragmentFile = fragmentFile;
	}
	
	// Reads a file from resources
	private String readFile(String fileName) {
		Path path = Paths.get(fileName);
		String content = "";
		try {
			content = Files.readString(path);
		}
		catch (IOException e) {
			System.out.println("Couldnt read the file " + fileName);
			System.out.println("    from path: " + path.toAbsolutePath());
		}
		return content;
	}
	
	// Loads and compiles a shader from a file
	private int loadShader(int type, String file) {
		int id = glCreateShader(type);
		glShaderSource(id, readFile(file));
		glCompileShader(id);
		
		if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
			System.out.println("Could Not Compile " + file);
			System.out.println(glGetShaderInfoLog(id));
		}
		
		return id;
	}
	
	public void create() {
		programID = glCreateProgram();
		
		vertexID = loadShader(GL_VERTEX_SHADER, vertexFile);
		fragmentID = loadShader(GL_FRAGMENT_SHADER, fragmentFile);
		
		glAttachShader(programID, vertexID);
		glAttachShader(programID, fragmentID);
		glLinkProgram(programID);
		glValidateProgram(programID);
	}
	
	public void use() {
		glUseProgram(programID);
	}
	
	public void stop() {
		glUseProgram(0);
	}
	
	public void delete() {
		stop();
		glDetachShader(programID, vertexID);
		glDetachShader(programID, fragmentID);
		glDeleteShader(vertexID);
		glDeleteShader(fragmentID);
		glDeleteProgram(programID);
	}

    /**
     * Sets a float uniform value in the shader.
     * 
     * @param name  The uniform variable name in the shader
     * @param value The float value to pass
     */
	public void setUniform1f(String name, float value) {
		int location = glGetUniformLocation(programID, name);
		if (location != -1) {
			glUniform1f(location, value);
		} else {
			//System.out.println("Uniform " + name + " not found in shader!");
		}
	}

    /**
     * Sets a float uniform value in the shader.
     * 
     * @param name  The uniform variable name in the shader
     * @param value The int value to pass
     */
	public void setUniform1i(String name, int value) {
		int location = glGetUniformLocation(programID, name);
		if (location != -1) {
			glUniform1f(location, value);
		} else {
			System.out.println("Uniform " + name + " not found in shader!");
		}
	}

	public void passChars(float[] chars) {
		int location = glGetUniformLocation(programID, "chars");
		if (location != -1) {
			glUniform1fv(location, chars);
		} else {
			//System.out.println("Uniform chars not found in shader!");
		}
	}
}

class Model {
	private float[] vertexArray;
	private int[] indices;
	
	private int vboID, iboID;
	
	public Model(float scale) {
		// Added simple color attribute, but it's static in this example
		vertexArray = new float[] {
			-1 * scale, 1 * scale, 0 * scale, 1, 1, 1, 1.0f,
			-1 * scale, -1 * scale, 0 * scale, 1, 1, 1, 1.0f,
			1 * scale, 1 * scale, 0 * scale,  1, 1, 1, 1.0f
		};
		
		indices = new int[] {
			0, 1, 2
		};
	}
	
	public void create() {
		vboID = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboID);
		
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertexArray.length);
		vertexBuffer.put(vertexArray).flip();
		glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		iboID = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboID);
		
		IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
		indexBuffer.put(indices).flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
		
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	public void delete() {
		glDeleteBuffers(vboID);
		glDeleteBuffers(iboID);
	}
	
	public void setPointers() {
		// Set the vertex attribute pointers:
		// position attribute (3 floats)
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.BYTES, 0);
		// color attribute (4 floats)
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.BYTES, 3 * Float.BYTES);
	}
	
	public int getVboID() {
		return vboID;
	}

	public int getIboID() {
		return iboID;
	}
	
	public int getVertexCount() {
		return vertexArray.length / 7; // 7 floats per vertex (3 pos + 4 color)
	}
	
	public int getIndexCount() {
		return indices.length;
	}

}

class Render {
	public static void render(int vaoID, Model triangle) {
		glBindVertexArray(vaoID);
		glBindBuffer(GL_ARRAY_BUFFER, triangle.getVboID());
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, triangle.getIboID());
		
		glEnableVertexAttribArray(0); // position
		glEnableVertexAttribArray(1); // color
		triangle.setPointers();
		
		glDrawElements(GL_TRIANGLES, triangle.getIndexCount(), GL_UNSIGNED_INT, 0);
		
		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

}

public class MG2 {
	private static String size;
	private static String playerName;

	public static void setSize(String size2) {
		size = size2;
	}
	
	public static void setPlayerName(String name) {
		playerName = name;	
	}

	private static class Window {
		public GameManager gameManager;
		private int mouseButtonLastFrame = 0;
		public double currentLogCooldown = 0f;
		public double logCooldown = 0.5f;
		
		private int width = 800;
		private int height = 800;
		
		private int vaoID;
		
		private Model triangle1;
		private Model triangle2;
		private Shader shader;
		
		private float delta;
		private long window;
		
		public Window() {
			int sizeInt = 5;
			switch (size) {
				case "4x4":
					sizeInt = 4;
				break;
				case "8x8":
					sizeInt = 8;
				break;
				case "10x10":
					sizeInt = 8;
				break;
			}
			gameManager = new GameManager(sizeInt, sizeInt, 4, 10, false);
		}


		public int pollMouse() {
			int mouseButton = glfwGetMouseButton(window, 0);
			// If mb1 was pressed this frame
			if (mouseButton == 1) {
				if (mouseButtonLastFrame == 0) {
					mouseButtonLastFrame = mouseButton;
					return 1;	
				}
				else {
					mouseButtonLastFrame = mouseButton;
					return 0;
				}
			}
			mouseButtonLastFrame = mouseButton;
			return 0;
		}
		
		public void run() {
			System.out.println("Trying to initalize the game window");
			init();
			gameManager.printSlotsConsole(0);
			loop();
		}
		
		public void init() {
			if (!glfwInit()) {
				throw new IllegalStateException("Unable to initialize GLFW");
			}
			
			glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
			glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
			
			window = glfwCreateWindow(width, height, 
					"Memory Game 2: My Head Hurts: My Raymarching Adventure: Two Titles is a bit-: Three titles", 0, 0);
			if (window == NULL) {
				throw new RuntimeException("Failed to create GLFW window");
			}
			
			glfwMakeContextCurrent(window);
			GL.createCapabilities();
			

			shader = new Shader();
			shader.create();
			
			vaoID = glGenVertexArrays();
			glBindVertexArray(vaoID);
			
			// Create two models with different scales
			triangle1 = new Model(1f);
			triangle1.create();
			triangle2 = new Model(-1f);
			triangle2.create();
			
			glfwShowWindow(window);

			System.out.println("Renderer: " + glGetString(GL_RENDERER));
			System.out.println("Vendor: " + glGetString(GL_VENDOR));

			System.out.println("OpenGL version: " + glGetString(GL_VERSION));

			System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "false");

		}
		
		private int floorInt(float n) {
			return Math.round( (float) Math.floor(n));
		}

		public void saveScore(boolean win) {
			try (FileWriter writer = new FileWriter("leaderboard.txt", true)) {
				ZonedDateTime currentDate = ZonedDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
				String formattedDate = currentDate.format(formatter);

				String winString = "lost";
				if (win) winString = "won";
				
				writer.write(playerName + " " + winString + " with " + gameManager.tries + " invalid guesses, at " + formattedDate + System.lineSeparator());
				writer.close();
			} catch (IOException e) {
				System.err.println("Error: Couldn't write to leaderboards file");
			}
		}
		
		public void loop() {

			// FPS cap
			int targetFPS = 24;
			double desiredFrameTime = 1.0 / targetFPS;

			// The rendering loop
			while (!glfwWindowShouldClose(window)) {
				double startTime = glfwGetTime();
				shader.use();
				
				// Clear screen
				glClearColor(1, 1, 1, 1);
				glClear(GL_COLOR_BUFFER_BIT);
				
				// Get current time since start of the program
				float time = (float) glfwGetTime();
				
				// Pass the time uniform to the shader
				shader.setUniform1f("time", time);

				shader.setUniform1f("time", time);
				shader.setUniform1f("arrayX", gameManager.gameSizeX);
				shader.setUniform1f("arrayY", gameManager.gameSizeY);

				float[] chars = new float[gameManager.gameSizeX * gameManager.gameSizeY];
				chars = gameManager.getCharsAsFloats(delta);
				shader.passChars(chars);

				double[] mouseX, mouseY;
				float finalMouseX, finalMouseY;

				mouseX = new double[1]; 
				mouseY = new double[1]; 

				glfwGetCursorPos(window, mouseX, mouseY);
				finalMouseX = (float) mouseX[0] / (width / 2) - 1;
				finalMouseY = (float) mouseY[0] / (height / 2) - 1;
				/*
				System.out.println("Mouse Coords Raw:");
				System.out.println("    X:" + mouseX[0]);
				System.out.println("    Y:" + mouseY[0]);

				System.out.println("Mouse Coords Transformed:");
				System.out.println("    X:" + finalMouseX);
				System.out.println("    Y:" + finalMouseY);
				*/

				shader.setUniform1f("mouseX", finalMouseX);
				shader.setUniform1f("mouseY", finalMouseY);

				// If mouse1 was clicked this frame
				if (pollMouse() == 1) {
					int clickedX = floorInt((finalMouseX + 1.0f) / 2.0f * (float) gameManager.gameSizeX);
					int clickedY = floorInt((finalMouseY + 1.0f) / 2.0f * (float) gameManager.gameSizeY);

					System.out.println("Clicked Coords: " + clickedX + ", " + clickedY);
					int clickedResult = gameManager.clicked(clickedX, clickedY);
					if (clickedResult == 1) {
						gameManager.currentTries = 0;
						if (gameManager.allRevealed()) {
							System.out.println("    Game Won!!!");
							saveScore(true);
							return;
						}
					}
					else if (clickedResult == 0) {
						gameManager.currentTries++;
						gameManager.tries++;
						if (gameManager.currentTries >= gameManager.maxTries) {
							System.out.println("    Game Joever!");
							saveScore(false);
							return;
						}
					}
				}

				// Render two triangles
				Render.render(vaoID, triangle1);
				Render.render(vaoID, triangle2);
				
				glfwSwapBuffers(window);
				glfwPollEvents();
				
				shader.stop();

				// End of frame timing
				double endTime = glfwGetTime();
				double frameTime = endTime - startTime;
				currentLogCooldown += frameTime;
				if (currentLogCooldown >= logCooldown) {
					currentLogCooldown = 0f;
					System.out.println("Start time: " + startTime * 1000 + "ms");
					System.out.println("End time: " + endTime * 1000 + "ms");
					System.out.println("    Frametime: " + frameTime * 1000 + "ms");
				}
				
				//System.out.println(frameTime*1000);

				// If the frame finished early, sleep the thread
				delta = (float) frameTime;
				if (frameTime < desiredFrameTime) {
					try {
						Thread.sleep((long) ((desiredFrameTime - frameTime) * 1000));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			// Cleanup
			glDeleteVertexArrays(vaoID);
			triangle1.delete();
			triangle2.delete();
			shader.delete();
			
			glfwTerminate();
		}

	}

	public static class SelectionWindow extends Application {
		public String selectedSize;
		public String playerName;

		@Override
		public void start(Stage primaryStage) {
			// Text field
			TextField textField = new TextField();
			textField.setPromptText("Enter Name:");

			// Create a ChoiceBox with three options
			ChoiceBox<String> choiceBox = new ChoiceBox<>();
			choiceBox.getItems().addAll("4x4", "8x8", "10x10");

			// Preselect "4x4"
			choiceBox.setValue("4x4");

			// Create a button to confirm the user's choice
			Button confirmButton = new Button("Start Game!");
			confirmButton.setOnAction(event -> {
				playerName = textField.getText();
				selectedSize = choiceBox.getValue();
				System.out.println("User selected: " + selectedSize);
				primaryStage.close();  // Close the window
			});

			// Arrange the ChoiceBox and Button horizontally
			HBox sizeSelectionLayout = new HBox(10);
			sizeSelectionLayout.getChildren().addAll(choiceBox, confirmButton);
			sizeSelectionLayout.setAlignment(Pos.CENTER);

			// Arrange all elements in a vertical layout
			VBox layout = new VBox(10);
			layout.getChildren().addAll(textField, sizeSelectionLayout);
			layout.setAlignment(Pos.CENTER);

			// Set up the scene and show the stage
			Scene scene = new Scene(layout, 165, 70); // Adjusted size for better spacing
			primaryStage.setScene(scene);
			primaryStage.setTitle("");
			primaryStage.show();
		}


		@Override
        public void stop() {
            // Called when the JavaFX application is about to shut down
            // Here, we pass our data to CombinedWindows
			MG2.setSize(selectedSize);
			MG2.setPlayerName(playerName);
        }
	}

	public static void main(String[] args) {
		Application.launch(SelectionWindow.class, args);
		System.out.println("Created Game Window with size " + size);
		Window window = new Window();
		window.run();
		System.out.println("Created Selection Window!");
	}
}