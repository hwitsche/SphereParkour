#version 330 core

layout(location = 0) in vec3 position;

uniform mat4 projection_matrix;
uniform mat4 view_matrix;

void main() {

    textureCoordinates = vec3(position.x -position.y, position.z);
    vec4 pos = projection_matrix * view_matrix * vec4(position, 1.0);
    gl_Position = pos.xyww;

}