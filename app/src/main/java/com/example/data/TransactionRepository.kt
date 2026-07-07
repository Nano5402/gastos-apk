package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByProfile(profileId: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByProfile(profileId)

    val allProfiles: Flow<List<Profile>> = transactionDao.getAllProfiles()

    suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertProfile(profile: Profile): Long {
        return transactionDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: Profile) {
        transactionDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: Profile) {
        transactionDao.deleteProfile(profile)
    }

    suspend fun deleteProfileById(profileId: Int) {
        transactionDao.deleteTransactionsByProfile(profileId)
        transactionDao.deleteProfileById(profileId)
    }
}
