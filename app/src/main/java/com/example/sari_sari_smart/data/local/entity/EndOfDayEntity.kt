package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.EndOfDayData

@Entity(tableName = "end_of_day_data")
data class EndOfDayEntity(
    @PrimaryKey val date: String,
    val cashInDrawer: Double = 0.0,
    val stockCheckDone: Boolean = false,
    val debtPaymentsDone: Boolean = false,
    val finished: Boolean = false,
    val recordedSales: Double = 0.0,
    val actualSales: Double = 0.0,
    val salesDiff: Double = 0.0,
    val costOfGoods: Double = 0.0,
    val profit: Double = 0.0
) {
    fun toDomainModel(): EndOfDayData = EndOfDayData(
        date = date,
        cashInDrawer = cashInDrawer,
        stockCheckDone = stockCheckDone,
        debtPaymentsDone = debtPaymentsDone,
        finished = finished,
        recordedSales = recordedSales,
        actualSales = actualSales,
        salesDiff = salesDiff,
        costOfGoods = costOfGoods,
        profit = profit
    )

    companion object {
        fun fromDomainModel(data: EndOfDayData): EndOfDayEntity = EndOfDayEntity(
            date = data.date,
            cashInDrawer = data.cashInDrawer,
            stockCheckDone = data.stockCheckDone,
            debtPaymentsDone = data.debtPaymentsDone,
            finished = data.finished,
            recordedSales = data.recordedSales,
            actualSales = data.actualSales,
            salesDiff = data.salesDiff,
            costOfGoods = data.costOfGoods,
            profit = data.profit
        )
    }
}
