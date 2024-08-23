package cga.exercise.components.light

import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f

class SpotLight(private val name: String, lightColor: Vector3f, position: Vector3f, var innerCone: Float, var outerCone: Float) : PointLight(name, lightColor, position), ISpotLight {

    init {
    }

    override fun bind(shaderProgram: ShaderProgram, viewMatrix: Matrix4f) {
        super.bind(shaderProgram)
        shaderProgram.setUniform("$name.Cone", Vector2f(innerCone, outerCone))
        shaderProgram.setUniform("$name.Direction", Vector3f(getWorldZAxis()).negate().mul(Matrix3f(viewMatrix)))
    }
}
