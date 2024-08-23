#version 330 core

#define MAX_POINT_LIGHTS 10
#define MAX_SPOT_LIGHTS 10

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 textureCoordinate;
layout(location = 2) in vec3 normal;

// uniforms
uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 proj_matrix;

uniform vec2 tcMultiplier;

// lights
struct PointLight
{
    vec3 Color;
    vec3 Position;
};
struct SpotLight
{
    vec3 Color;
    vec3 Position;
    vec2 Cone;
    vec3 Direction;
};
uniform PointLight pointLight[MAX_POINT_LIGHTS];
uniform int numPointLights;
uniform SpotLight spotLight[MAX_SPOT_LIGHTS];
uniform int numSpotLights;

// out data
out struct VertexData
{
    vec2 textureCoordinate;
    vec3 normal;
    // Vectors needed for lighting calculations. Must not be normalized in the vertex shader (interpolation would mess them up).
    vec3 toCamera;
    vec3 toPointLight[MAX_POINT_LIGHTS];
    vec3 toSpotLight[MAX_SPOT_LIGHTS];
} vertexData;

void main(){
    // Combined model-view matrix
    mat4 modelview = view_matrix * model_matrix;
    // Transform vertex position to view space
    vec4 viewpos = modelview * vec4(position, 1.0f);
    // View vector is -viewpos in view space (unnormalized!)
    vertexData.toCamera = -viewpos.xyz;
    // Calculate toLight vectors for all point lights (unnormalized!)
    for (int i = 0; i < numPointLights; i++){
        vertexData.toPointLight[i] = (view_matrix * vec4(pointLight[i].Position, 1.0)).xyz - viewpos.xyz;
    }
    // Calculate toLight vectors for all spot lights (unnormalized!)
    for (int i = 0; i < numSpotLights; i++) {
        vertexData.toSpotLight[i] = (view_matrix * vec4(spotLight[i].Position, 1.0)).xyz - viewpos.xyz;
    }
    gl_Position = proj_matrix * viewpos;
    // Calculate surface normal and texture coordinate
    vertexData.normal = (inverse(transpose(modelview)) * vec4(normal, 0.0f)).xyz;
    vertexData.textureCoordinate = textureCoordinate * tcMultiplier;
}