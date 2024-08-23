package cga.exercise.components.light

import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Vector3f

open class PointLight(private val name: String, var lightColor: Vector3f, position: Vector3f) : Transformable(), IPointLight {

    init {
        translate(position)
    }

    override fun bind(shaderProgram: ShaderProgram) {
        shaderProgram.setUniform("$name.Color", lightColor)
        shaderProgram.setUniform("$name.Position", getWorldPosition())
    }
}