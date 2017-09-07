package util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.smartregister.Context;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Photo;
import org.smartregister.domain.ProfileImage;
import org.smartregister.path.R;
import org.smartregister.path.application.VaccinatorApplication;
import org.smartregister.repository.ImageRepository;

import java.util.Collections;

import shared.BaseUnitTest;
  

/**
 * Created by ona on 28/08/2017.
 */
@PrepareForTest({VaccinatorApplication.class})
public class ImageUtilsTest extends BaseUnitTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Mock
    private CommonPersonObjectClient commonPersonObjectClient;

    @Mock
    private VaccinatorApplication vaccinatorApplication;

    @Mock
    private Context context;

    @Mock
    private ImageRepository imageRepository;

    @Before
    public void setUp() {

        org.mockito.MockitoAnnotations.initMocks(this);
    
    }


    @Test
    public void profileImageResourceByGenderWithEmptyStringParameterReturnsDefaultResource() {
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender(""), R.drawable.child_boy_infant);
    }

    @Test
    public void profileImageResourceByGenderWithMaleParameterReturnsMaleResource() {
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender("male"), R.drawable.child_boy_infant);
    }

    @Test
    public void profileImageResourceByGenderWithFemaleParameterReturnsFemaleResource() {
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender("female"), R.drawable.child_girl_infant);
    }

    @Test
    public void profileImageResourceByGenderWithTransgenderParameterReturnsTransgenderResource() {
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender("transgender"), R.drawable.child_transgender_inflant);
    }

    @Test
    public void profileImageResourceByGenderObjectWithMaleGenderParameterReturnsMaleResource() {
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender(org.opensrp.api.constants.Gender.MALE), R.drawable.child_boy_infant);
    }

    @Test
    public void profileImageResourceByGenderWithFemaleObjectReturnsFemaleResource() {
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender(org.opensrp.api.constants.Gender.FEMALE), R.drawable.child_girl_infant);
    }

    @Test
    public void profileImageResourceByGenderWithNullObjectParameterReturnsTransgenderResource() {
        org.opensrp.api.constants.Gender gender = null;
        org.junit.Assert.assertEquals(ImageUtils.profileImageResourceByGender(gender), R.drawable.child_transgender_inflant);
    }

    @Test
    public void imageUtilsClassConstructorReturnsNonNullObjectOnInstantiation() {
        org.junit.Assert.assertNotNull(new ImageUtils());
    }

    @Test
    public void profilePhotoByClientReturnsDefaultInfantBoyPhoto() {
        PowerMockito.mockStatic(VaccinatorApplication.class);
        PowerMockito.when(VaccinatorApplication.getInstance()).thenReturn(vaccinatorApplication);
        PowerMockito.when(VaccinatorApplication.getInstance().context()).thenReturn(context);
        PowerMockito.when(VaccinatorApplication.getInstance().context().imageRepository()).thenReturn(imageRepository);
        PowerMockito.when(VaccinatorApplication.getInstance().context().imageRepository().findByEntityId(org.mockito.Matchers.anyString())).thenReturn(null);
        Photo photo = ImageUtils.profilePhotoByClient(commonPersonObjectClient);
        org.junit.Assert.assertNotNull(photo);
        org.junit.Assert.assertEquals(photo.getResourceId(), R.drawable.child_boy_infant);
    }

    @Test
    public void profilePhotoByClientReturnsCorrectPhotoFilePathForCorrespondingClient() {
        PowerMockito.mockStatic(VaccinatorApplication.class);
        PowerMockito.when(VaccinatorApplication.getInstance()).thenReturn(vaccinatorApplication);
        PowerMockito.when(VaccinatorApplication.getInstance().context()).thenReturn(context);
        PowerMockito.when(VaccinatorApplication.getInstance().context().imageRepository()).thenReturn(imageRepository);
        ProfileImage profileImage = new ProfileImage();
        String imagePath = "/dummy/test/path/image.png";
        String dummyCaseId = "4400";
        profileImage.setFilepath(imagePath);
        PowerMockito.when(VaccinatorApplication.getInstance().context().imageRepository().findByEntityId(dummyCaseId)).thenReturn(profileImage);
        commonPersonObjectClient = new CommonPersonObjectClient(dummyCaseId, Collections.<String, String>emptyMap(), "Test Name");
        commonPersonObjectClient.setCaseId(dummyCaseId);
        Photo photo = ImageUtils.profilePhotoByClient(commonPersonObjectClient);
        org.junit.Assert.assertNotNull(photo);
        org.junit.Assert.assertEquals(imagePath, photo.getFilePath());
    }
}
