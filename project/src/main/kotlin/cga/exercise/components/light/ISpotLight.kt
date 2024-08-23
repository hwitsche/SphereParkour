package cga.exercise.components.light

import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f

interface ISpotLight {
    fun bind(shaderProgram: ShaderProgram, viewMatrix: Matrix4f)
}