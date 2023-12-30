package com.asaad27

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PhysicSim : ApplicationAdapter() {
    companion object {
        private const val BALL_RADIUS = 0.2f
        private const val PIXELS_TO_METERS = 100f
        private const val CIRCLE_RADIUS = 6f
        private val INITIAL_VELOCITY_X = listOf(-4f, -3f, -2f, -1f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f)
        private val INITIAL_VELOCITY_Y = listOf(4f, 5f, 6f, 7f, 9f, 10f, 11f, 12f, 13f, 14f, 20f)
        private const val WORLD_GRAVITY = 0f
        private const val BALL_DENSITY = 0.8f
        private const val BALL_RESTITUTION = 1f
        private const val TIME_STEP = 0.8f / 60f
        private const val VELOCITY_ITERATIONS = 6
        private const val POSITION_ITERATIONS = 2
        private const val RANDOM_CHANCE_THRESHOLD = 20
    }

    private lateinit var world: World
    private lateinit var balls: MutableList<Body>
    private lateinit var camera: OrthographicCamera
    private lateinit var shapeRenderer: ShapeRenderer
    private val ballCreationQueue = mutableListOf<Vector2>()

    override fun create() {
        initializeGraphics()
        initializePhysics()
    }

    private fun initializeGraphics() {
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position[0f, 0f] = 0f
        }
        shapeRenderer = ShapeRenderer()
    }

    private fun initializePhysics() {
        world = World(Vector2(0f, WORLD_GRAVITY), true)
        balls = mutableListOf(createBall(Vector2(0f, 0f)))
        createCircleBody()
        setupCollisionListener()
    }

    private fun setupCollisionListener() {
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val (userDataTypeA, userDataTypeB) = Pair(contact.fixtureA.userData, contact.fixtureB.userData)

                val ball = when {
                    userDataTypeA is UserDataType.Ball && userDataTypeB is UserDataType.CircleEdge -> userDataTypeA
                    userDataTypeA is UserDataType.CircleEdge && userDataTypeB is UserDataType.Ball -> userDataTypeB
                    else -> return
                } as? UserDataType.Ball ?: return

                if (ball.isInCollision || Random.nextInt(100) > RANDOM_CHANCE_THRESHOLD) return

                ballCreationQueue.add(Vector2(0f, 0f))
                ball.isInCollision = true
            }

            override fun endContact(contact: Contact?) {
                contact ?: return
                val (userDataTypeA, userDataTypeB) = Pair(contact.fixtureA.userData, contact.fixtureB.userData)
                val isBallAndCircleEdgeCollision =
                    (userDataTypeA is UserDataType.CircleEdge && userDataTypeB is UserDataType.Ball) ||
                            (userDataTypeA is UserDataType.Ball && userDataTypeB is UserDataType.CircleEdge)
                if (isBallAndCircleEdgeCollision) {
                    val ball = (userDataTypeA as? UserDataType.Ball) ?: (userDataTypeB as? UserDataType.Ball)
                    ball?.isInCollision = false
                }
            }

            override fun preSolve(contact: Contact?, oldManifold: Manifold?) {
                /* no-op */
            }

            override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {
                /* no-op */
            }
        })
    }

    override fun render() {
        clearScreen()
        stepWorld()
        processBallCreationQueue()
        renderGraphics()
    }

    private fun clearScreen() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
    }

    private fun stepWorld() {
        world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
    }

    private fun processBallCreationQueue() {
        ballCreationQueue.forEach { position ->
            balls.add(createBall(position))
        }
        ballCreationQueue.clear()
    }

    private fun renderGraphics() {
        drawCircle()
        drawBalls()
    }

    private fun drawCircle() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.WHITE
        shapeRenderer.circle(0f, 0f, CIRCLE_RADIUS * PIXELS_TO_METERS, 360)
        shapeRenderer.end()
    }

    private fun drawBalls() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        balls.forEach { ball ->
            val ballUserData = ball.userData as? UserDataType.Ball
            val color = ballUserData?.color ?: Color.WHITE
            shapeRenderer.color = color
            shapeRenderer.circle(
                ball.position.x * PIXELS_TO_METERS,
                ball.position.y * PIXELS_TO_METERS,
                BALL_RADIUS * PIXELS_TO_METERS
            )
        }
        shapeRenderer.end()
    }

    private fun createBall(position: Vector2): Body {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            this.position.set(position)
        }
        val body = world.createBody(bodyDef)
        val circleShape = CircleShape().apply {
            radius = BALL_RADIUS
        }
        val fixtureDef = FixtureDef().apply {
            shape = circleShape
            density = BALL_DENSITY
            restitution = BALL_RESTITUTION
        }
        val randomColor = Color(
            Math.random().toFloat(),
            Math.random().toFloat(),
            Math.random().toFloat(),
            1f
        )
        body.createFixture(fixtureDef).userData = UserDataType.Ball(randomColor, BALL_RADIUS)
        circleShape.dispose()
        body.linearVelocity = Vector2(INITIAL_VELOCITY_X.random(), INITIAL_VELOCITY_Y.random())
        return body.apply { userData = UserDataType.Ball(randomColor, BALL_RADIUS) }
    }

    private fun createCircleBody() {
        val bodyDef = BodyDef().apply { type = BodyDef.BodyType.StaticBody }
        val body = world.createBody(bodyDef)
        val chainShape = ChainShape()
        val vertices = Array(36) { i ->
            val angle = 2 * Math.PI * i / 36
            Vector2(CIRCLE_RADIUS * cos(angle).toFloat(), CIRCLE_RADIUS * sin(angle).toFloat())
        }
        chainShape.createLoop(vertices)
        body.createFixture(chainShape, 0f).userData = UserDataType.CircleEdge
        chainShape.dispose()
    }

    override fun dispose() {
        world.dispose()
        shapeRenderer.dispose()
    }
}

sealed class UserDataType {
    data object CircleEdge : UserDataType()
    data class Ball(
        val color: Color,
        val radius: Float,
        var isInCollision: Boolean = false
    ) : UserDataType()
}
