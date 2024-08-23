package cga.exercise.components.light

import cga.exercise.components.shader.ShaderProgram

interface IPointLight {
    fun bind(shaderProgram: ShaderProgram)
}