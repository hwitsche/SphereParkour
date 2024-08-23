#version 330 core
#extension GL_NV_shadow_samplers_cube : enable

out vec4 color;

in vec3 textureCoordinates;
uniform samplerCube skybox;

void main() {

    color = texture(skybox, textureCoordinates);
}