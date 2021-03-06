package uk.nhs.nhsx.isolationpayment

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.amazonaws.services.dynamodbv2.model.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.core.aws.dynamodb.DynamoAttributes.*
import uk.nhs.nhsx.isolationpayment.model.IsolationToken
import uk.nhs.nhsx.isolationpayment.model.TokenStateInternal
import java.time.Instant
import java.time.Period
import java.util.*
import java.util.function.Supplier

class IsolationPersistenceLocalTest {

    private val dbClient = DynamoDBEmbedded.create().amazonDynamoDB()
    private val tableName = "isolation_tokens_payment_table"
    private val persistence = IsolationPaymentPersistence(dbClient, tableName)
    private val dynamoDB = DynamoDB(dbClient)
    private val isolationTokenTable = dynamoDB.getTable(tableName)
    private val clock = Supplier { Instant.parse("2020-12-01T00:00:00Z") }

    @BeforeEach
    fun setup() {
        val attributeDefinitions: MutableList<AttributeDefinition> = ArrayList()
        attributeDefinitions.add(AttributeDefinition().withAttributeName("tokenId").withAttributeType("S"))

        val keySchema: MutableList<KeySchemaElement> = ArrayList()
        keySchema.add(KeySchemaElement().withAttributeName("tokenId").withKeyType(KeyType.HASH))

        val request = CreateTableRequest()
            .withTableName(tableName)
            .withKeySchema(keySchema)
            .withAttributeDefinitions(attributeDefinitions)
            .withProvisionedThroughput(ProvisionedThroughput(100L, 100L))
        dbClient.createTable(request)
    }

    @Test
    fun `gets isolation payment token`() {
        val token = getIsolationToken()

        val itemMap = itemMapFrom(token)

        val request = PutItemRequest()
            .withTableName(tableName)
            .withItem(itemMap)

        dbClient.putItem(request)

        persistence
            .getIsolationToken(token.tokenId)
            .map {
                assertThat(it.tokenId).isEqualTo(token.tokenId)
                assertThat(it.tokenStatus).isEqualTo(TokenStateInternal.INT_CREATED.value)
                assertThat(it.riskyEncounterDate).isEqualTo(token.riskyEncounterDate)
                assertThat(it.isolationPeriodEndDate).isEqualTo(token.isolationPeriodEndDate)
                assertThat(it.createdTimestamp).isEqualTo(token.createdTimestamp)
                assertThat(it.updatedTimestamp).isEqualTo(token.updatedTimestamp)
                assertThat(it.validatedTimestamp).isEqualTo(token.validatedTimestamp)
                assertThat(it.consumedTimestamp).isEqualTo(token.consumedTimestamp)
                assertThat(it.expireAt).isEqualTo(token.expireAt)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `creates isolation payment token`() {
        val token = getIsolationToken()

        persistence.insertIsolationToken(token)

        val item = isolationTokenTable.getItem("tokenId", token.tokenId)

        assertThat(item.getString("tokenStatus")).isEqualTo(TokenStateInternal.INT_CREATED.value)
        assertThat(item.getLong("expireAt")).isEqualTo(token.expireAt)
        assertThat(item.getLong("createdTimestamp")).isNotNull
    }

    @Test
    fun `updates isolation payment token`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        token.tokenStatus = TokenStateInternal.INT_UPDATED.value
        persistence.updateIsolationToken(token, TokenStateInternal.INT_CREATED)

        persistence
            .getIsolationToken(token.tokenId)
            .map {
                assertThat(it.tokenId).isEqualTo(token.tokenId)
                assertThat(it.tokenStatus).isEqualTo(TokenStateInternal.INT_UPDATED.value)
                assertThat(it.riskyEncounterDate).isEqualTo(token.riskyEncounterDate)
                assertThat(it.isolationPeriodEndDate).isEqualTo(token.isolationPeriodEndDate)
                assertThat(it.createdTimestamp).isEqualTo(token.createdTimestamp)
                assertThat(it.updatedTimestamp).isEqualTo(token.updatedTimestamp)
                assertThat(it.validatedTimestamp).isEqualTo(token.validatedTimestamp)
                assertThat(it.consumedTimestamp).isEqualTo(token.consumedTimestamp)
                assertThat(it.expireAt).isEqualTo(token.expireAt)
            }
            .orElseThrow { RuntimeException("Token not found") }
    }

    @Test
    fun `update throws when token id condition fails`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        token.tokenStatus = TokenStateInternal.INT_UPDATED.value
        token.tokenId = "random-id"

        assertThatThrownBy {
            persistence.updateIsolationToken(token, TokenStateInternal.INT_CREATED) // wrong token id
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    @Test
    fun `update throws when status condition fails`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        token.tokenStatus = TokenStateInternal.INT_UPDATED.value

        assertThatThrownBy {
            persistence.updateIsolationToken(token, TokenStateInternal.INT_UPDATED) // wrong current status
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    @Test
    fun `deletes isolation payment token`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        persistence.deleteIsolationToken(token.tokenId, TokenStateInternal.INT_CREATED)

        assertThat(persistence.getIsolationToken(token.tokenId)).isEmpty
    }

    @Test
    fun `delete throws when token id condition fails`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        assertThatThrownBy {
            persistence.deleteIsolationToken("random-id", TokenStateInternal.INT_CREATED) // wrong token id
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    @Test
    fun `delete throws when status condition fails`() {
        val token = getIsolationToken()
        persistence.insertIsolationToken(token)

        assertThatThrownBy {
            persistence.deleteIsolationToken(token.tokenId, TokenStateInternal.INT_UPDATED) // wrong current status
        }.isInstanceOf(ConditionalCheckFailedException::class.java)
    }

    private fun getIsolationToken(): IsolationToken {
        val tokenId = "tokenId"
        val createdDate = clock.get().epochSecond
        val ttl = clock.get().plus(Period.ofWeeks(4)).epochSecond
        return IsolationToken(tokenId, TokenStateInternal.INT_CREATED.value, 0, 0, createdDate, 0, 0, 0, ttl)
    }

    private fun itemMapFrom(token: IsolationToken): Map<String, AttributeValue> =
        mapOf(
            "tokenId" to stringAttribute(token.tokenId),
            "tokenStatus" to stringAttribute(token.tokenStatus),
            "riskyEncounterDate" to numericNullableAttribute(token.riskyEncounterDate),
            "isolationPeriodEndDate" to numericNullableAttribute(token.isolationPeriodEndDate),
            "createdTimestamp" to numericAttribute(token.createdTimestamp),
            "updatedTimestamp" to numericNullableAttribute(token.updatedTimestamp),
            "validatedTimestamp" to numericNullableAttribute(token.validatedTimestamp),
            "consumedTimestamp" to numericNullableAttribute(token.consumedTimestamp),
            "expireAt" to numericAttribute(token.expireAt)
        )
}