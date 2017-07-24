package org.smartregister.path.handler;

import org.smartregister.domain.form.FormSubmission;
import org.smartregister.path.service.WomanService;
import org.smartregister.service.formsubmissionhandler.FormSubmissionHandler;

/**
 * Created by muhammad.ahmed@ihsinformatics.com on 08-Dec-15.
 */
public class WomanFollowupHandler implements FormSubmissionHandler {

    private WomanService womanService;

    public WomanFollowupHandler(WomanService womanService) {
        this.womanService = womanService;
    }

    @Override
    public void handle(FormSubmission submission) {
        womanService.followup(submission);
    }
}
