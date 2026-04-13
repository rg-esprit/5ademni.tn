<?php

namespace App\Form;

use App\Entity\Job;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;

class JobType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'attr' => ['placeholder' => 'e.g. Senior PHP Developer', 'minlength' => 5, 'maxlength' => 100],
            ])
            ->add('company', TextType::class, [
                'attr' => ['placeholder' => 'e.g. Tech Corp', 'minlength' => 2, 'maxlength' => 100],
            ])
            ->add('location', TextType::class, [
                'attr' => ['placeholder' => 'e.g. Tunis, Remote', 'minlength' => 2, 'maxlength' => 100],
            ])
            ->add('category', ChoiceType::class, [
                'choices' => [
                    'IT & Software' => 'IT & Software',
                    'Design & Creative' => 'Design & Creative',
                    'Marketing' => 'Marketing',
                    'Writing & Translation' => 'Writing & Translation',
                    'Sales & Support' => 'Sales & Support',
                    'Other' => 'Other',
                ],
                'placeholder' => 'Choose a category',
            ])
            ->add('jobType', ChoiceType::class, [
                'choices' => [
                    'Full-time' => 'Full-time',
                    'Part-time' => 'Part-time',
                    'Contract' => 'Contract',
                    'Freelance' => 'Freelance',
                    'Internship' => 'Internship',
                ],
                'placeholder' => 'Choose job type',
                'required' => true,
            ])
            ->add('salaryRange', TextType::class, [
                'required' => false,
                'attr' => [
                    'placeholder' => 'e.g. 1500 - 2500',
                    'pattern' => '^[\d\s\-\.]+$',
                    'title' => 'Please enter numbers and optional dash for range (e.g. 1500 - 2500)'
                ],
            ])
            ->add('description', TextareaType::class, [
                'attr' => ['rows' => 5, 'placeholder' => 'Describe the job responsibilities...', 'minlength' => 20],
            ])
            ->add('requirements', TextareaType::class, [
                'required' => false,
                'attr' => ['rows' => 5, 'placeholder' => 'List the requirements, skills, etc...', 'maxlength' => 2000],
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Job::class,
        ]);
    }
}
