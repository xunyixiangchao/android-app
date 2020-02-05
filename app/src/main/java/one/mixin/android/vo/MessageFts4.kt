package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Entity(tableName = "messages_fts4")
@Fts4(notIndexed = ["message_id"], tokenizer = FtsOptions.TOKENIZER_UNICODE61)
class MessageFts4(
    @ColumnInfo(name = "message_id")
    var messageId: String,
    @ColumnInfo(name = "content")
    var content: String?
)