package org.opensrp.path.handler;

import org.opensrp.domain.form.FormSubmission;
import org.opensrp.path.service.WomanService;
import org.opensrp.service.formSubmissionHandler.FormSubmissionHandler;

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
