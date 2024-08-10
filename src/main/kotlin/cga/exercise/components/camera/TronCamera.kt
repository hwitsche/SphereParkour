package cga.exercise.components.camera

import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f

/**
 * Created by Fabian on 16.09.2017.
 */

class TronCamera(var aspectratio: Aspectratio = Aspectratio.WIDESCREEN,
                 var fov: Float = Math.toRadians(90.0).toFloat(),
                 var near: Float = 0.1f,
                 var far: Float = 100.0f) : Transformable() {


    //we do a view matrix update only when needed
    fun calculateViewMatrix(): Matrix4f {
        return Matrix4f().lookAt(getWorldPosition(), getWorldPosition().sub(getWorldZAxis()), getWorldYAxis())
    }

    fun calculateProjectionMatrix(): Matrix4f {
        return Matrix4f().perspective(fov, aspectratio.ratio, near, far)
    }

    fun bind(shader: ShaderProgram) {
        shader.setUniform("view_matrix", calculateViewMatrix(), false);
        shader.setUniform("proj_matrix", calculateProjectionMatrix(), false);
    }
}

class Aspectratio(val ratio : Float) {

    companion object {
        fun custom(width: Int, height: Int) : Aspectratio {
            return Aspectratio(width/height.toFloat())
        }
        val WIDESCREEN = custom(16,9)
        val OLDSCOOL = custom(4,3)
    }
}