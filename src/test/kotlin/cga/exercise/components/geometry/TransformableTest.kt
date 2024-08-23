package cga.exercise.components.geometry

import org.joml.Math.PI
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


internal class TransformableTest {
    private val comparisonDelta = 0.000001f

    @Test
    fun testGetModelMatrix() {
        val transformable = Transformable()
        assertTrue(
                transformable.getModelMatrix().equals(Matrix4f(), comparisonDelta),
                "model matrix not initialized to identity matrix")

        val mat = Matrix4f(
                1.0f, 2.0f, 3.0f, 4.0f,
                5.0f, 1.0f, 6.0f, 7.0f,
                8.0f, 9.0f, 1.0f, 11.0f,
                12.0f, 13.0f, 14.0f, 32.0f)
        val transformableMat = Transformable(mat)
        assertTrue(transformableMat.getModelMatrix().equals(Matrix4f(mat), comparisonDelta),
                "model matrix not initialized to specified matrix")

        assertTrue(transformableMat.getModelMatrix() !== transformableMat.getModelMatrix(),
            "getModelMatrix() should return a copy")
    }

    @Test
    fun testGetWorldModelMatrix() {
        val matChild = Matrix4f(
                1.0f, 2.0f, 3.0f, 4.0f,
                5.0f, 1.0f, 6.0f, 7.0f,
                8.0f, 9.0f, 1.0f, 11.0f,
                12.0f, 13.0f, 14.0f, 32.0f)
        val transformableChild = Transformable(matChild)
        assertTrue(transformableChild.parent == null,
                "unless specified, parent should be initialized with null")

        val matParent = Matrix4f(
                1.0f, 0.0f, 2.0f, 0.0f,
                0.0f, 2.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)
        val transformableParent = Transformable(matParent)
        transformableChild.parent = transformableParent

        val matParentParent = Matrix4f(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 5.0f, 3.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)
        val transformableParentParent = Transformable(matParentParent)
        transformableParent.parent = transformableParentParent

        val matParentParentChild = Matrix4f(matParentParent).mul(matParent).mul(matChild)

        val getWorldModelMatrix4fReturnValue = transformableChild.getWorldModelMatrix()
        assertTrue(getWorldModelMatrix4fReturnValue.equals(Matrix4f(matParentParentChild), comparisonDelta),
                "concatenation of child and parent matrices seems to be incorrect.")

        assertTrue(transformableChild.getWorldModelMatrix().equals(getWorldModelMatrix4fReturnValue, comparisonDelta),
            "repeated calls of getWorldModelMatrix() should return the same result.")

        transformableParent.parent = transformableParent
        assertFailsWith<StackOverflowError>("method should be implemented recursively") {
            transformableChild.getWorldModelMatrix()
        }
    }

    @Test
    fun testRotate() {
        val transformable = Transformable()
        transformable.rotate(0f, PI.toFloat()/2f, 0f)

        val matResult = Matrix4f(
                -0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, -0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)

        assertTrue(matResult.equals(transformable.getModelMatrix(), comparisonDelta))
    }

    @Test
    fun testRotateAroundPoint() {
        val mat = Matrix4f(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 1.0f)
        val transformable = Transformable(mat)
        transformable.rotateAroundPoint(0f, PI.toFloat()/2f, 0f, Vector3f(0f,0f,0f))

        val matResult = Matrix4f(
                0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f, 1.0f)

        assertTrue(matResult.equals(transformable.getModelMatrix(), comparisonDelta))
    }

    @Test
    fun testTranslate() {
        val mat = Matrix4f(
                0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)
        val transformable = Transformable(mat)
        transformable.translate(Vector3f(2f, 3f, 4f))

        val matResult = Matrix4f(
                0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 0.0f,
                4.0f, 3.0f, -2.0f, 1.0f)

        assertTrue(matResult.equals(transformable.getModelMatrix(), comparisonDelta))
    }

    @Test
    fun testPreTranslate() {
        val mat = Matrix4f(
                0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)
        val transformable = Transformable(mat)
        transformable.preTranslate(Vector3f(2f, 3f, 4f))

        val matResult = Matrix4f(
                0.0f, 0.0f, -1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f, 0.0f,
                2.0f, 3.0f, 4.0f, 1.0f)

        assertTrue(matResult.equals(transformable.getModelMatrix(), comparisonDelta))
    }

    @Test
    fun testScale() {
        val transformable = Transformable()
        transformable.scale(Vector3f(2f, 3f, 4f))

        val matResult = Matrix4f(
                2.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 3.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 4.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)

        assertTrue(matResult.equals(transformable.getModelMatrix(), comparisonDelta))
    }

    @Test
    fun testGetPosition() {
        val mat = Matrix4f(
                1.0f, 2.0f, 3.0f, 4.0f,
                5.0f, 1.0f, 6.0f, 7.0f,
                8.0f, 9.0f, 1.0f, 11.0f,
                12.0f, 13.0f, 14.0f, 32.0f)
        val transformable = Transformable(mat)

        assertTrue(Vector3f(
                mat.m30(), mat.m31(), mat.m32()
        ).equals(transformable.getPosition(), comparisonDelta))
    }

    @Test
    fun testGetWorldPosition() {
        val matChild = Matrix4f(
                1.0f, 2.0f, 3.0f, 4.0f,
                5.0f, 1.0f, 6.0f, 7.0f,
                8.0f, 9.0f, 1.0f, 11.0f,
                12.0f, 13.0f, 14.0f, 32.0f)
        val transformableChild = Transformable(matChild)

        val matParent = Matrix4f(
                1.0f, 0.0f, 2.0f, 0.0f,
                0.0f, 2.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)
        val transformableParent = Transformable(matParent)
        transformableChild.parent = transformableParent
        val matParentChild = Matrix4f(matParent).mul(matChild)

        assertTrue(Vector3f(
                matParentChild.m30(), matParentChild.m31(), matParentChild.m32()
        ).equals(transformableChild.getWorldPosition(), comparisonDelta))
    }

    @Test
    fun testGetAxes() {
        val mat = Matrix4f(
                1.0f, 2.0f, 3.0f, 4.0f,
                5.0f, 1.0f, 6.0f, 7.0f,
                8.0f, 9.0f, 1.0f, 11.0f,
                12.0f, 13.0f, 14.0f, 32.0f)
        val transformable = Transformable(mat)
        assertTrue(Vector3f(
                mat.m00(), mat.m01(), mat.m02()
        ).normalize().equals(transformable.getXAxis(), comparisonDelta))
        assertTrue(Vector3f(
                mat.m10(), mat.m11(), mat.m12()
        ).normalize().equals(transformable.getYAxis(), comparisonDelta))
        assertTrue(Vector3f(
                mat.m20(), mat.m21(), mat.m22()
        ).normalize().equals(transformable.getZAxis(), comparisonDelta))
    }

    @Test
    fun testGetWorldAxes() {
        val matChild = Matrix4f(
                1.0f, 2.0f, 3.0f, 4.0f,
                5.0f, 1.0f, 6.0f, 7.0f,
                8.0f, 9.0f, 1.0f, 11.0f,
                12.0f, 13.0f, 14.0f, 32.0f)
        val transformableChild = Transformable(matChild)

        val matParent = Matrix4f(
                1.0f, 0.0f, 2.0f, 0.0f,
                0.0f, 2.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f)
        val transformableParent = Transformable(matParent)
        transformableChild.parent = transformableParent
        val matParentChild = Matrix4f(matParent).mul(matChild)

        assertTrue(Vector3f(
                matParentChild.m00(), matParentChild.m01(), matParentChild.m02()
        ).normalize().equals(transformableChild.getWorldXAxis(), comparisonDelta))
        assertTrue(Vector3f(
                matParentChild.m10(), matParentChild.m11(), matParentChild.m12()
        ).normalize().equals(transformableChild.getWorldYAxis(), comparisonDelta))
        assertTrue(Vector3f(
                matParentChild.m20(), matParentChild.m21(), matParentChild.m22()
        ).normalize().equals(transformableChild.getWorldZAxis(), comparisonDelta))
    }
}