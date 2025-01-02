package net.thevenot.comwatt.ui.site

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.lifecycle.viewmodel.compose.viewModel
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.model.Address
import net.thevenot.comwatt.model.Agreements
import net.thevenot.comwatt.model.Phone
import net.thevenot.comwatt.model.Profile
import net.thevenot.comwatt.model.Site
import net.thevenot.comwatt.model.User
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SiteChooserScreen(
    session: Session,
    dataRepository: DataRepository,
    viewModel: SiteChooserViewModel = viewModel { SiteChooserViewModel(session, dataRepository) },
    onSiteSelected: (Int) -> Unit = {}
) {
    val sites by viewModel.sites.collectAsState()
    val user by viewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkSiteSelection(onSiteSelected) {
            viewModel.loadSites()
        }
    }

    SiteChooserContent(sites, user) { site ->
        viewModel.saveSiteId(site.id)
        onSiteSelected(site.id)
    }
}

@Composable
fun SiteChooserContent(sites: List<Site>, user: User?, onSiteClick: (Site) -> Unit = {}) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(
            AppTheme.dimens.paddingSmall,
            Alignment.CenterVertically
        ),
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(AppTheme.dimens.paddingNormal)
    ) {
        items(sites) { site ->
            SiteCard(site, user, onSiteClick)
        }
    }
}

@Composable
fun SiteCard(site: Site, user: User?, onSiteClick: (Site) -> Unit = {}) {
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
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))
            user?.let {
                Text(
                    text = "${it.firstName} ${it.lastName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = site.address.formatAddress(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview
@Composable
private fun SiteChooserPreview() {
    val sampleSites = listOf(
        Site(
            id = 1,
            name = "Site 1",
            description = "Description 1",
            createDate = "2021-01-01",
            updateDate = "2021-01-02",
            ownerAssignDate = "2021-01-03",
            threePhase = true,
            address = Address("123 Main St", "12345", "Paris", "France"),
            currency = "USD",
            language = "en",
            metric = "metric",
            timezone = "GMT",
            siteUid = "UID1",
            supplyNumber = "SN1",
            status = "Active",
            owner = "Owner 1",
            accessType = "Full",
            state = "State 1",
            siteKind = "Kind 1"
        ),
        Site(
            id = 2,
            name = "Site 2",
            description = "Description 2",
            createDate = "2021-02-01",
            updateDate = "2021-02-02",
            ownerAssignDate = "2021-02-03",
            threePhase = false,
            address = Address("456 Elm St", "67890", "Montreal", "Canada"),
            currency = "EUR",
            language = "fr",
            metric = "metric",
            timezone = "CET",
            siteUid = "UID2",
            supplyNumber = "SN2",
            status = "Inactive",
            owner = "Owner 2",
            accessType = "Read",
            state = "State 2",
            siteKind = "Kind 2"
        )
    )
    val user = User(
        id = 1,
        login = "user1",
        firstName = "John",
        lastName = "Doe",
        email = "john.doe@example.com",
        newEmail = null,
        pseudonym = null,
        profile = Profile(
            id = 1,
            label = "Admin",
            code = "ADMIN",
            authorities = null
        ),
        address = Address("789 Oak St", "54321", "City", "Country"),
        phone = Phone("1234567890", "1"),
        mobilePhone = "0987654321",
        currency = "USD",
        language = "en",
        activated = true,
        deleted = false,
        company = "Company",
        createDate = "2021-01-01",
        updateDate = "2021-01-02",
        agreements = Agreements(
            termsAndConditionsEndUser = true,
            termsAndConditionsInstaller = null,
            dataProcessing = true,
            noDisclosure = null,
            newsletter = true
        ),
        uuid = "uuid-1234"
    )

    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SiteChooserContent(sites = sampleSites, user = user)
        }
    }
}