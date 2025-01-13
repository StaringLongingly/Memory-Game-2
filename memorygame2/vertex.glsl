#version 460 core

//we just need to rasterize a quad for raymarching so vertex shading is very simple

layout (location = 0) in vec3 position; //vertex positions
out vec2 fragCord;  //pixel coordinate 

void main() {
    // Do not delete everything breaks
	gl_Position = vec4(position, 1);
    fragCord = position.xy;
}