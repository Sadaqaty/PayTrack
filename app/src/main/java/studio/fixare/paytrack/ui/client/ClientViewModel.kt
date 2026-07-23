package studio.fixare.paytrack.ui.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import studio.fixare.paytrack.data.Client
import studio.fixare.paytrack.data.ClientStatus
import studio.fixare.paytrack.data.PayTrackRepository
import studio.fixare.paytrack.data.PaymentCycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientViewModel(private val repository: PayTrackRepository) : ViewModel() {
    val clients: StateFlow<List<Client>> = repository.getAllClients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addClient(
        name: String,
        projectName: String,
        contractStartDate: Long,
        paymentCycle: PaymentCycle,
        rate: Double,
        currency: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertClient(
                Client(
                    name = name,
                    projectName = projectName,
                    contractStartDate = contractStartDate,
                    paymentCycle = paymentCycle,
                    rate = rate,
                    currency = currency,
                    notes = notes
                )
            )
        }
    }
    
    suspend fun getClientById(id: Int): Client? {
        return repository.getClientById(id)
    }
    
    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client)
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }
}
