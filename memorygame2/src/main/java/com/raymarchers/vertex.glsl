#version 330 core

layout (location = 0) in vec3 position;
out vec2 fragCord;
uniform float time;

void main() {
    // Do not delete everything breaks
	gl_Position = vec4(position, 1);
    fragCord = position.xy;
}