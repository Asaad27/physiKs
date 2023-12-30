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

class PhysicSim : ApplicationAdapter() {
    companion object {
        private const val BALL_RADIUS = 0.5f
        private const val PIXELS_TO_METERS = 100f
        private const val CIRCLE_RADIUS = 5f
        private val INITIAL_VELOCITY = Vector2(0f, 10f)
    }

    private lateinit var world: World
    private lateinit var balls: MutableList<Body>
    private lateinit var debugRenderer: Box2DDebugRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var shapeRenderer: ShapeRenderer
    private val ballCreationQueue = mutableListOf<Vector2>()

    override fun create() {
        initializeGraphics()
        initializePhysics()
    }

    private fun initializeGraphics() {
        debugRenderer = Box2DDebugRenderer()
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position[0f, 0f] = 0f
        shapeRenderer = ShapeRenderer()
    }

    private fun initializePhysics() {
        world = World(Vector2(0f, -10f), true)
        balls = mutableListOf()
        balls.add(createBall(Vector2(0f, 0f)))
        createCircleBody()
        setupCollisionListener()
    }

    private fun setupCollisionListener() {
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val fixtureA = contact.fixtureA
                val fixtureB = contact.fixtureB
                if ((fixtureA.userData is HashMap<*, *> && fixtureB.userData == "circleEdge") ||
                    (fixtureA.userData == "circleEdge" && fixtureB.userData is HashMap<*, *>)) {

                    val ballFixture = if (fixtureA.userData is HashMap<*, *>) fixtureA else fixtureB
                    val ballData = ballFixture.userData as HashMap<String, Any>

                    if (ballData["type"] == "ball" && ballData["spawned"] == false) {
                        ballCreationQueue.add(Vector2(0f, 0f))
                        ballData["spawned"] = true
                    }
                }
            }

            override fun endContact(contact: Contact?) {

            }

            override fun preSolve(contact: Contact?, oldManifold: Manifold?) {

            }

            override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {

            }
        })
    }

    override fun render() {
        clearScreen()
        stepWorld()
        processBallCreationQueue()
        renderPhysics()
        renderGraphics()
    }

    private fun clearScreen() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
    }

    private fun stepWorld() {
        world.step(Gdx.graphics.deltaTime, 6, 2)
    }

    private fun processBallCreationQueue() {
        ballCreationQueue.forEach { position ->
            balls.add(createBall(position))
        }
        ballCreationQueue.clear()
    }

    private fun renderPhysics() {
        camera.update()
        debugRenderer.render(world, camera.combined)
    }

    private fun renderGraphics() {
        drawCircle()
        drawBalls()
    }

    private fun drawCircle() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.WHITE
        shapeRenderer.circle(0f, 0f, CIRCLE_RADIUS * PIXELS_TO_METERS, 1024)
        shapeRenderer.end()
    }

    private fun drawBalls() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.WHITE
        balls.forEach { ball ->
            val color = Color(
                Math.random().toFloat(),
                Math.random().toFloat(),
                Math.random().toFloat(),
                1f
            )
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
            density = 2.0f
            restitution = 1.0f
        }
        body.createFixture(fixtureDef).userData = hashMapOf("type" to "ball", "spawned" to false)
        circleShape.dispose()
        body.linearVelocity = INITIAL_VELOCITY
        return body
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
        body.createFixture(chainShape, 0f).userData = "circleEdge"
        chainShape.dispose()
    }

    override fun dispose() {
        world.dispose()
        debugRenderer.dispose()
        shapeRenderer.dispose()
    }
}
