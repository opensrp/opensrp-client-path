package org.smartregister.path.handler;

import org.smartregister.domain.form.FormSubmission;
import org.smartregister.path.service.ChildService;
import org.smartregister.service.formsubmissionhandler.FormSubmissionHandler;

/**
 * Created by muhammad.ahmed@ihsinformatics.com on 29-Oct-15.
 */
public class ChildFollowupHandler implements FormSubmissionHandler {

    private ChildService childService;

    public ChildFollowupHandler(ChildService childService) {
        this.childService = childService;
    }

    @Override
    public void handle(FormSubmission submission) {
        childService.followup(submission);
    }
}
