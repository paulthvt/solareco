package net.thevenot.comwatt.ui.site

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.AddressDto
import net.thevenot.comwatt.model.AgreementsDto
import net.thevenot.comwatt.model.PhoneDto
import net.thevenot.comwatt.model.ProfileDto
import net.thevenot.comwatt.model.SiteDto
import net.thevenot.comwatt.model.UserDto
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme

@Composable
fun SiteChooserScreen(
    dataRepository: DataRepository,
    viewModel: SiteChooserViewModel = viewModel { SiteChooserViewModel(dataRepository) },
    onSiteSelected: (Int) -> Unit = {}
) {
    val sites by viewModel.sites.collectAsState()
    val user by viewModel.userDto.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkSiteSelection(onSiteSelected) {
            viewModel.loadSites()
        }
    }

    SiteChooserContent(sites, user) { site ->
        site.id?.let {
            viewModel.saveSiteId(it)
            onSiteSelected(it)
        }
    }
}

@Composable
fun SiteChooserContent(sites: List<SiteDto>, userDto: UserDto?, onSiteClick: (SiteDto) -> Unit = {}) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(
            AppTheme.dimens.paddingSmall,
            Alignment.CenterVertically
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.paddingNormal)
    ) {
        items(sites) { site ->
            SiteCard(site, userDto, onSiteClick)
        }
    }
}

@Composable
fun SiteCard(site: SiteDto, userDto: UserDto?, onSiteClick: (SiteDto) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable { onSiteClick(site) }
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimens.paddingNormal)
        ) {
            Row {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Site Icon",
                )
                Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
                Text(
                    text = site.name ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))
            userDto?.let {
                Text(
                    text = "${it.firstName} ${it.lastName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = site.address?.formatAddress() ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview
@Composable
private fun SiteChooserPreview() {
    val userDto = UserDto(
        id = 1,
        login = "user1",
        firstName = "John",
        lastName = "Doe",
        email = "john.doe@example.com",
        newEmail = null,
        pseudonym = null,
        profile = ProfileDto(
            id = 1,
            label = "Admin",
            code = "ADMIN",
            authorities = listOf()
        ),
        address = AddressDto("789 Oak St", "54321", "City", "Country"),
        phone = PhoneDto("1234567890", "1"),
        mobilePhone = "0987654321",
        currency = "USD",
        language = "en",
        activated = true,
        deleted = false,
        company = "Company",
        createDate = "2021-01-01",
        updateDate = "2021-01-02",
        agreements = AgreementsDto(
            termsAndConditionsEndUser = true,
            termsAndConditionsInstaller = null,
            dataProcessing = true,
            noDisclosure = null,
            newsletter = true
        ),
        uuid = "uuid-1234"
    )
    val sampleSites = listOf(
        SiteDto(
            id = 1,
            name = "Site 1",
            description = "Description 1",
            createDate = "2021-01-01",
            updateDate = "2021-01-02",
            ownerAssignDate = "2021-01-03",
            threePhase = true,
            address = AddressDto("123 Main St", "12345", "Paris", "France"),
            currency = "USD",
            language = "en",
            metric = "metric",
            timezone = "GMT",
            siteUid = "UID1",
            supplyNumber = "SN1",
            status = "Active",
            owner = userDto,
            accessType = "Full",
            state = "State 1",
            siteKind = "Kind 1"
        ),
        SiteDto(
            id = 2,
            name = "Site 2",
            description = "Description 2",
            createDate = "2021-02-01",
            updateDate = "2021-02-02",
            ownerAssignDate = "2021-02-03",
            threePhase = false,
            address = AddressDto("456 Elm St", "67890", "Montreal", "Canada"),
            currency = "EUR",
            language = "fr",
            metric = "metric",
            timezone = "CET",
            siteUid = "UID2",
            supplyNumber = "SN2",
            status = "Inactive",
            owner = userDto,
            accessType = "Read",
            state = "State 2",
            siteKind = "Kind 2"
        )
    )

    ComwattTheme {
        Surface {
            SiteChooserContent(sites = sampleSites, userDto = userDto)
        }
    }
}