package com.raymarchers;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.BufferUtils;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.*;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;

// A Slot is a letter with some extra info
class Slot {
    public char symbol;
    public boolean peek;
    public boolean revealed;

    public Slot(char symbol) {
        this.symbol = symbol;
        this.peek = false;
        this.revealed = false;
    }

    public Slot() {
        this.peek = false;
        this.revealed = false;
    }

    public String toString() {
        if (peek || revealed) {
            peek = false;
            return Character.toString(symbol);
        } else {
            return "*";
        }
    }
}

// Handles initialization and gameplay
class GameManager {
    public Slot[][] slots;

    public GameManager(int gameSizeX, int gameSizeY, int shuffleCount, boolean reveal, boolean consolePrint) {
        System.out.println("Creating a New Game!");
        slots = new Slot[gameSizeY][gameSizeX];

        int totalSlots = gameSizeX * gameSizeY;
        int pairCount = totalSlots / 2;

        // Generate pairs of random characters
        List<Character> charList = new ArrayList<Character>();
        Random random = new Random();
        for (int i = 0; i < pairCount; i++) {
            char randomChar = (char) (random.nextInt(26) + 'A');
            charList.add(randomChar);
            charList.add(randomChar);
        }

        // Shuffle the generated characters
        Collections.shuffle(charList, random);

        // Fill the slots
        int index = 0;
        for (int i = 0; i < slots.length; i++) {
            for (int j = 0; j < slots[i].length; j++) {
                slots[i][j] = new Slot(charList.get(index));
                slots[i][j].revealed = reveal;
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

        char tmp = slots[x1][y1].symbol;
        slots[x1][y1].symbol = slots[x2][y2].symbol;
        slots[x2][y2].symbol = tmp;
    }

    public void gameplayLoop() {
        Scanner in = new Scanner(System.in);
        int tries = 0;
        while (!allRevealed()) {
            printSlotsConsole();
            tries++;

            int comparison;
            int x1, x2, y1, y2;
            do {
                System.out.print("Give the first row and column: ");
                x1 = in.nextInt();
                y1 = in.nextInt();

                System.out.print("Give the second row and column: ");
                x2 = in.nextInt();
                y2 = in.nextInt();

                comparison = compareSlots(x1, y1, x2, y2);
            } while (comparison == -1);

            if (comparison == 0) {
                slots[x1][y1].peek = true;
                slots[x2][y2].peek = true;
            } else if (comparison == 1) {
                slots[x1][y1].revealed = true;
                slots[x2][y2].revealed = true;
            }
        }
        in.close();
        System.out.println("Successfully revealed all letters!");
        System.out.println("It took you " + tries + " tries!");
    }

    public int compareSlots(int x1, int y1, int x2, int y2) {
        // Check bounds
        if (x1 < 0 || x1 >= slots.length ||
            x2 < 0 || x2 >= slots.length ||
            y1 < 0 || y1 >= slots[0].length ||
            y2 < 0 || y2 >= slots[0].length) {
            System.out.println("Invalid coordinates! Please try again:");
            return -1;
        }

        // Check if same slot chosen twice
        if (x1 == x2 && y1 == y2) {
            System.out.println("You cannot give the same coordinates for both letters. Try again..");
            return -1;
        }

        // Compare symbols
        if (slots[x1][y1].symbol == slots[x2][y2].symbol) {
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
    public void printSlotsConsole() {
        System.out.print("    ");
        for (int j = 0; j < slots[0].length; j++) {
            System.out.print(j + "   ");
        }
        System.out.println();

        for (int i = 0; i < slots.length; i++) {
            System.out.print(i + "  ");
            for (int j = 0; j < slots[i].length; j++) {
                System.out.print(slots[i][j]);
                if (j < slots[i].length - 1) System.out.print(" | ");
            }
            System.out.println();
        }
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
		String string = "";
		File file = new File(this.getClass().getResource(fileName).getFile());
		Scanner scan = null;
		try {
			scan = new Scanner(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		while (scan != null && scan.hasNextLine()) {
			string += scan.nextLine() + "\n";
		}
		
		return string;
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
			System.out.println("Uniform " + name + " not found in shader!");
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

class Window {
	
	private static Window instance = null;
	
	private int width = 800;
	private int height = 600;
	
	private int vaoID;
	
	private Model triangle1;
	private Model triangle2;
	private Shader shader;
	
	private long window;
	
	private Window() {}
	
	public static Window get() {
		if (instance == null) {
			instance = new Window();
		}
		
		return instance;
	}
	
	public void run() {
		init();
		loop();
	}
	
	public void init() {
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		
		window = glfwCreateWindow(width, height, 
		        "Memory Game 2: Pass Time Uniform Example", 0, 0);
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
	}
	
	public void loop() {
		// The rendering loop
		while (!glfwWindowShouldClose(window)) {
			shader.use();
			
			// Clear screen
			glClearColor(1, 1, 1, 1);
			glClear(GL_COLOR_BUFFER_BIT);
			
			// Get current time since start of the program
			float time = (float) glfwGetTime();
			
			// Pass the time uniform to the shader
			shader.setUniform1f("time", time);
			
			// Render two triangles
			Render.render(vaoID, triangle1);
			Render.render(vaoID, triangle2);
			
			glfwSwapBuffers(window);
			glfwPollEvents();
			
			shader.stop();
		}
		
		// Cleanup
		glDeleteVertexArrays(vaoID);
		triangle1.delete();
		triangle2.delete();
		shader.delete();
		
		glfwTerminate();
	}

}

// Responsible for starting the game
public class Main {
    public static void main(String args[]) {
        // If you want to run the text-based game:
        // new GameManager(4, 5, 10, false, false).gameplayLoop();
        
        // Otherwise, run the windowed application with the shader:
        Window.get().run();
    }
}
